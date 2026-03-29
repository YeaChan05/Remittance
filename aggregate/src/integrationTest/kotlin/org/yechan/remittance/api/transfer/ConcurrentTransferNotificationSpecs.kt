package org.yechan.remittance.api.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.AggregateTransferTestFixtures
import org.yechan.remittance.EmailGenerator
import org.yechan.remittance.PasswordGenerator
import org.yechan.remittance.account.NotificationSubscriptionHandler
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringBootTest(
    classes = [AggregateApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@Import(
    org.yechan.remittance.AggregateTransferTestFixturesConfig::class,
    RecordingNotificationTestConfig::class,
)
class ConcurrentTransferNotificationSpecs {
    @Autowired
    lateinit var fixtures: AggregateTransferTestFixtures

    @Autowired
    lateinit var notificationStore: RecordingNotificationStore

    @Autowired
    lateinit var notificationSubscriptionHandler: NotificationSubscriptionHandler

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @BeforeEach
    fun setUp() {
        fixtures.reset()
        notificationStore.clear()
    }

    @Test
    fun `동시에 1000건의 이체가 같은 수신 계좌로 들어와도 잔액이 정확히 누적되고 수신 알림이 1000건 전달된다`() {
        // Arrange
        val scenario = arrangeConcurrentTransferScenario()
        val executor = Executors.newFixedThreadPool(64)

        try {
            // Act
            val responses = executeConcurrentTransfers(
                executor = executor,
                accessToken = scenario.senderAuth.accessToken,
                fromAccountId = scenario.senderAccount.accountId,
                toAccountId = scenario.receiverAccount.accountId,
                amount = scenario.transferAmount,
                idempotencyKeys = scenario.idempotencyKeys,
            )

            // Assert
            val transferIds = assertSuccessfulTransfers(responses, scenario.requestCount)
            assertBalances(scenario)
            assertPersistedTransferRecords(scenario)
            assertNotificationEventuallyDelivered(scenario)
            val notificationTransferIds = assertDeliveredNotifications(scenario)
            assertThat(notificationTransferIds).isSubsetOf(transferIds.filterNotNull().toSet())
        } finally {
            executor.shutdownNow()
        }
    }

    private fun arrangeConcurrentTransferScenario(): ConcurrentTransferScenario {
        val requestCount = 1_000
        val transferAmount = BigDecimal.valueOf(1_000L)
        val transferFee = BigDecimal.TEN
        val senderInitialBalance = BigDecimal.valueOf(2_000_000L)

        val receiverEmail = EmailGenerator.generate()
        val senderEmail = EmailGenerator.generate()
        val receiverPassword = PasswordGenerator.generate()
        val senderPassword = PasswordGenerator.generate()

        fixtures.registerMember("receiver", receiverEmail, receiverPassword)
        fixtures.registerMember("sender", senderEmail, senderPassword)

        val receiverAuth = fixtures.login(receiverEmail, receiverPassword)
        val senderAuth = fixtures.login(senderEmail, senderPassword)

        val receiverAccount = fixtures.createAccount(receiverAuth.accessToken, "receiver-main")
        val senderAccount = fixtures.createAccount(senderAuth.accessToken, "sender-main")

        val initialDeposit = fixtures.deposit(
            senderAuth.accessToken,
            senderAccount.accountId,
            senderInitialBalance,
        )
        assertThat(initialDeposit.status).isEqualTo("SUCCEEDED")
        assertThat(initialDeposit.errorCode).isNull()

        notificationSubscriptionHandler.subscribe(receiverAuth.memberId)
        val idempotencyKeys = fixtures.issueTransferIdempotencyKeys(
            senderAuth.accessToken,
            requestCount,
        )

        return ConcurrentTransferScenario(
            requestCount = requestCount,
            transferAmount = transferAmount,
            transferFee = transferFee,
            senderInitialBalance = senderInitialBalance,
            senderAuth = senderAuth,
            receiverAuth = receiverAuth,
            senderAccount = senderAccount,
            receiverAccount = receiverAccount,
            transferCountBefore = fixtures.countTransfers(),
            ledgerCountBefore = fixtures.countLedgers(),
            outboxCountBefore = fixtures.countOutboxEvents(),
            processedEventCountBefore = fixtures.countProcessedEvents(),
            idempotencyKeys = idempotencyKeys,
        )
    }

    private fun assertSuccessfulTransfers(
        responses: List<TransferCallResult>,
        requestCount: Int,
    ): List<Long?> {
        assertThat(responses).hasSize(requestCount)
        assertThat(responses.all { it.httpStatus == 200 }).isTrue()
        assertThat(responses.map { it.body.status }.all { it == "SUCCEEDED" }).isTrue()
        assertThat(responses.map { it.body.errorCode }.all { it == null }).isTrue()

        val transferIds = responses.map { it.body.transferId }
        assertThat(transferIds).doesNotContainNull()
        assertThat(transferIds.filterNotNull().distinct()).hasSize(requestCount)
        return transferIds
    }

    private fun assertBalances(scenario: ConcurrentTransferScenario) {
        assertThat(
            fixtures.loadAccountBalance(scenario.receiverAccount.accountId),
        ).isEqualByComparingTo(
            scenario.transferAmount.multiply(BigDecimal.valueOf(scenario.requestCount.toLong())),
        )
        assertThat(
            fixtures.loadAccountBalance(scenario.senderAccount.accountId),
        ).isEqualByComparingTo(
            scenario.senderInitialBalance.subtract(
                scenario.transferAmount.add(scenario.transferFee)
                    .multiply(BigDecimal.valueOf(scenario.requestCount.toLong())),
            ),
        )
    }

    private fun assertPersistedTransferRecords(scenario: ConcurrentTransferScenario) {
        assertThat(fixtures.countTransfers()).isEqualTo(
            scenario.transferCountBefore + scenario.requestCount.toLong(),
        )
        assertThat(fixtures.countLedgers()).isEqualTo(
            scenario.ledgerCountBefore + scenario.requestCount * 2L,
        )
        assertThat(fixtures.countOutboxEvents()).isEqualTo(
            scenario.outboxCountBefore + scenario.requestCount.toLong(),
        )
    }

    private fun assertNotificationEventuallyDelivered(scenario: ConcurrentTransferScenario) {
        eventually(Duration.ofSeconds(10), Duration.ofMillis(100)) {
            assertThat(fixtures.countSentOutboxEvents()).isGreaterThan(0)
            assertThat(fixtures.countProcessedEvents()).isGreaterThan(
                scenario.processedEventCountBefore,
            )
            assertThat(notificationStore.sentCount()).isGreaterThan(0)
        }
    }

    private fun assertDeliveredNotifications(
        scenario: ConcurrentTransferScenario,
    ): Set<Long> {
        val recordedPayloads: List<RecordingNotificationStore.RecordedPayload> =
            notificationStore.sentPayloads()
        val notificationPayloads: List<Map<String, Any?>> = recordedPayloads.map { payload ->
            objectMapper.convertValue(payload.payload, notificationPayloadTypeReference)
        }
        val notificationTypes: List<String> = notificationPayloads
            .map { payload -> payload.getValue("type").toString() }
            .distinct()
        val notificationAmounts: List<BigDecimal> = notificationPayloads
            .map { payload -> BigDecimal(payload.getValue("amount").toString()) }
            .distinct()
        val notificationFromAccountIds: List<Long> = notificationPayloads
            .map { payload -> (payload.getValue("fromAccountId") as Number).toLong() }
            .distinct()
        val notificationTransferIds: Set<Long> = notificationPayloads
            .map { payload -> (payload.getValue("transferId") as Number).toLong() }
            .toSet()

        assertThat(recordedPayloads).isNotEmpty()
        assertThat(notificationTypes).contains("TRANSFER_RECEIVED")
        assertThat(notificationAmounts).contains(scenario.transferAmount)
        assertThat(notificationFromAccountIds).contains(scenario.senderAccount.accountId)

        return notificationTransferIds
    }

    private fun executeConcurrentTransfers(
        executor: ExecutorService,
        accessToken: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
        idempotencyKeys: List<String>,
    ): List<TransferCallResult> {
        val startGate = CountDownLatch(1)
        val tasks = idempotencyKeys.map { idempotencyKey ->
            CompletableFuture.supplyAsync(
                {
                    startGate.await()
                    executeTransfer(
                        accessToken,
                        idempotencyKey,
                        fromAccountId,
                        toAccountId,
                        amount,
                    )
                },
                executor,
            )
        }

        startGate.countDown()
        return tasks.map { it.join() }
    }

    private fun executeTransfer(
        accessToken: String,
        idempotencyKey: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
    ): TransferCallResult {
        val response: AggregateTransferTestFixtures.TransferResponse = fixtures.transfer(
            accessToken,
            idempotencyKey,
            fromAccountId,
            toAccountId,
            amount,
        )
        return TransferCallResult(httpStatus = 200, body = response)
    }

    private fun eventually(
        timeout: Duration,
        interval: Duration,
        assertions: () -> Unit,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastError: AssertionError? = null

        while (System.nanoTime() < deadline) {
            try {
                assertions()
                return
            } catch (error: AssertionError) {
                lastError = error
                Thread.sleep(interval.toMillis())
            }
        }

        throw lastError ?: AssertionError("Condition was not satisfied within $timeout")
    }

    private data class TransferCallResult(
        val httpStatus: Int,
        val body: AggregateTransferTestFixtures.TransferResponse,
    )

    private data class ConcurrentTransferScenario(
        val requestCount: Int,
        val transferAmount: BigDecimal,
        val transferFee: BigDecimal,
        val senderInitialBalance: BigDecimal,
        val senderAuth: AggregateTransferTestFixtures.AuthInfo,
        val receiverAuth: AggregateTransferTestFixtures.AuthInfo,
        val senderAccount: AggregateTransferTestFixtures.AccountInfo,
        val receiverAccount: AggregateTransferTestFixtures.AccountInfo,
        val transferCountBefore: Long,
        val ledgerCountBefore: Long,
        val outboxCountBefore: Long,
        val processedEventCountBefore: Long,
        val idempotencyKeys: List<String>,
    )

    private companion object {
        val notificationPayloadTypeReference = object : TypeReference<Map<String, Any?>>() {}
    }
}
