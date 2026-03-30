package org.yechan.remittance.api.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.EmailGenerator
import org.yechan.remittance.PasswordGenerator
import org.yechan.remittance.account.AccountCreateUseCase
import org.yechan.remittance.account.AccountProps
import org.yechan.remittance.account.NotificationSubscriptionHandler
import org.yechan.remittance.member.MemberCreateUseCase
import org.yechan.remittance.member.MemberProps
import org.yechan.remittance.transfer.IdempotencyKeyCreateProps
import org.yechan.remittance.transfer.IdempotencyKeyCreateUseCase
import org.yechan.remittance.transfer.IdempotencyKeyProps
import org.yechan.remittance.transfer.OutboxEventProps
import org.yechan.remittance.transfer.TransferCreateUseCase
import org.yechan.remittance.transfer.TransferEventPublishUseCase
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferRequestProps
import org.yechan.remittance.transfer.TransferResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [AggregateApplication::class],
    properties = ["transfer.outbox.publisher.enabled=false"],
)
@Import(RecordingNotificationTestConfig::class)
class ConcurrentTransferNotificationSpecs {
    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var memberCreateUseCase: MemberCreateUseCase

    @Autowired
    lateinit var accountCreateUseCase: AccountCreateUseCase

    @Autowired
    lateinit var idempotencyKeyCreateUseCase: IdempotencyKeyCreateUseCase

    @Autowired
    lateinit var transferCreateUseCase: TransferCreateUseCase

    @Autowired
    lateinit var transferEventPublishUseCase: TransferEventPublishUseCase

    @Autowired
    lateinit var notificationStore: RecordingNotificationStore

    @Autowired
    lateinit var notificationSubscriptionHandler: NotificationSubscriptionHandler

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    private val nextAccountNumber = AtomicInteger(1)

    @BeforeEach
    fun setUp() {
        nextAccountNumber.set(1)
        notificationStore.clear()
        reset()
    }

    @Test
    fun `동시에 1000건의 이체가 같은 수신 계좌로 들어와도 잔액이 정확히 누적되고 수신 알림이 1000건 전달된다`() {
        val scenario = arrangeConcurrentTransferScenario()
        val executor = Executors.newFixedThreadPool(64)

        try {
            val responses = executeConcurrentTransfers(
                executor = executor,
                memberId = scenario.sender.memberId,
                fromAccountId = scenario.senderAccount.accountId,
                toAccountId = scenario.receiverAccount.accountId,
                amount = scenario.transferAmount,
                idempotencyKeys = scenario.idempotencyKeys,
            )

            val transferIds = assertSuccessfulTransfers(responses, scenario.requestCount)
            assertBalances(scenario)
            assertPersistedTransferRecords(scenario)
            val publishedCount = publishAllTransferEvents()
            assertThat(publishedCount).isEqualTo(scenario.requestCount)
            assertThat(countSentOutboxEvents()).isEqualTo(
                scenario.sentOutboxCountBefore + scenario.requestCount.toLong(),
            )
            assertNotificationEventuallyDelivered(scenario)

            val notificationTransferIds = assertDeliveredNotifications(scenario)
            assertThat(notificationTransferIds).isEqualTo(transferIds.filterNotNull().toSet())
        } finally {
            executor.shutdownNow()
        }
    }

    private fun arrangeConcurrentTransferScenario(): ConcurrentTransferScenario {
        val requestCount = 1_000
        val transferAmount = BigDecimal.valueOf(1_000L)
        val transferFee = feeFor(transferAmount)
        val senderInitialBalance = BigDecimal.valueOf(2_000_000L)

        val receiver = createMember("receiver")
        val sender = createMember("sender")
        val receiverAccount = createAccount(receiver.memberId, "receiver-main")
        val senderAccount = createAccount(sender.memberId, "sender-main")

        val initialDeposit = transferCreateUseCase.transfer(
            sender.memberId,
            issueIdempotencyKey(sender.memberId, IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT),
            TestTransferRequestProps.deposit(senderAccount.accountId, senderInitialBalance),
        )
        assertThat(initialDeposit.status).isEqualTo(TransferProps.TransferStatusValue.SUCCEEDED)
        assertThat(initialDeposit.errorCode).isNull()

        notificationSubscriptionHandler.subscribe(receiver.memberId)

        return ConcurrentTransferScenario(
            requestCount = requestCount,
            transferAmount = transferAmount,
            transferFee = transferFee,
            senderInitialBalance = senderInitialBalance,
            sender = sender,
            receiver = receiver,
            senderAccount = senderAccount,
            receiverAccount = receiverAccount,
            transferCountBefore = countTransfers(),
            ledgerCountBefore = countLedgers(),
            outboxCountBefore = countOutboxEvents(),
            sentOutboxCountBefore = countSentOutboxEvents(),
            processedEventCountBefore = countProcessedEvents(),
            idempotencyKeys = List(requestCount) {
                issueIdempotencyKey(sender.memberId, IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER)
            },
        )
    }

    private fun createMember(name: String): MemberSeed {
        val model = memberCreateUseCase.register(
            TestMemberProps(
                name = name,
                email = EmailGenerator.generate(),
                password = PasswordGenerator.generate(),
            ),
        )
        return MemberSeed(requireNotNull(model.memberId))
    }

    private fun createAccount(
        memberId: Long,
        accountName: String,
    ): AccountSeed {
        val accountNumber = nextAccountNumber.getAndIncrement().toString().padStart(12, '0')
        val model = accountCreateUseCase.create(
            TestAccountProps(
                memberId = memberId,
                bankCode = "090",
                accountNumber = accountNumber,
                accountName = accountName,
                balance = BigDecimal.ZERO,
            ),
        )
        return AccountSeed(requireNotNull(model.accountId))
    }

    private fun issueIdempotencyKey(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
    ): String = idempotencyKeyCreateUseCase.create(
        TestIdempotencyKeyCreateProps(memberId, scope),
    ).idempotencyKey

    private fun assertSuccessfulTransfers(
        responses: List<TransferResult>,
        requestCount: Int,
    ): List<Long?> {
        assertThat(responses).hasSize(requestCount)
        assertThat(responses.map { it.status }).allMatch {
            it == TransferProps.TransferStatusValue.SUCCEEDED
        }
        assertThat(responses.map { it.errorCode }).allMatch { it == null }

        val transferIds = responses.map { it.transferId }
        assertThat(transferIds).doesNotContainNull()
        assertThat(transferIds.filterNotNull().distinct()).hasSize(requestCount)
        return transferIds
    }

    private fun assertBalances(scenario: ConcurrentTransferScenario) {
        assertThat(loadAccountBalance(scenario.receiverAccount.accountId)).isEqualByComparingTo(
            scenario.transferAmount.multiply(BigDecimal.valueOf(scenario.requestCount.toLong())),
        )
        assertThat(loadAccountBalance(scenario.senderAccount.accountId)).isEqualByComparingTo(
            scenario.senderInitialBalance.subtract(
                scenario.transferAmount.add(scenario.transferFee)
                    .multiply(BigDecimal.valueOf(scenario.requestCount.toLong())),
            ),
        )
    }

    private fun assertPersistedTransferRecords(scenario: ConcurrentTransferScenario) {
        assertThat(countTransfers()).isEqualTo(
            scenario.transferCountBefore + scenario.requestCount.toLong(),
        )
        assertThat(countLedgers()).isEqualTo(
            scenario.ledgerCountBefore + scenario.requestCount * 2L,
        )
        assertThat(countOutboxEvents()).isEqualTo(
            scenario.outboxCountBefore + scenario.requestCount.toLong(),
        )
        assertThat(countNewOutboxEvents()).isEqualTo(scenario.requestCount.toLong())
    }

    private fun publishAllTransferEvents(): Int {
        var published = 0

        while (true) {
            val batch = transferEventPublishUseCase.publish(null)
            if (batch == 0) {
                assertThat(countNewOutboxEvents()).isZero()
                return published
            }
            published += batch
        }
    }

    private fun assertNotificationEventuallyDelivered(scenario: ConcurrentTransferScenario) {
        eventually(Duration.ofSeconds(20), Duration.ofMillis(100)) {
            assertThat(countProcessedEvents()).isEqualTo(
                scenario.processedEventCountBefore + scenario.requestCount.toLong(),
            )
            assertThat(notificationStore.sentCount()).isEqualTo(scenario.requestCount)
        }
    }

    private fun assertDeliveredNotifications(
        scenario: ConcurrentTransferScenario,
    ): Set<Long> {
        val recordedPayloads = notificationStore.sentPayloads()
        val notificationPayloads: List<Map<String, Any?>> = recordedPayloads.map { payload ->
            objectMapper.convertValue(payload.payload, notificationPayloadTypeReference)
        }
        val notificationTypes = notificationPayloads
            .map { payload -> payload.getValue("type").toString() }
            .distinct()
        val notificationAmounts = notificationPayloads
            .map { payload -> BigDecimal(payload.getValue("amount").toString()) }
            .distinct()
        val notificationFromAccountIds = notificationPayloads
            .map { payload -> (payload.getValue("fromAccountId") as Number).toLong() }
            .distinct()
        val notificationTransferIds = notificationPayloads
            .map { payload -> (payload.getValue("transferId") as Number).toLong() }
            .toSet()

        assertThat(recordedPayloads).hasSize(scenario.requestCount)
        assertThat(notificationTypes).containsExactly("TRANSFER_RECEIVED")
        assertThat(notificationAmounts).containsExactly(scenario.transferAmount)
        assertThat(notificationFromAccountIds).containsExactly(scenario.senderAccount.accountId)
        assertThat(notificationTransferIds).hasSize(scenario.requestCount)

        return notificationTransferIds
    }

    private fun executeConcurrentTransfers(
        executor: ExecutorService,
        memberId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
        idempotencyKeys: List<String>,
    ): List<TransferResult> {
        val startGate = CountDownLatch(1)
        val tasks = idempotencyKeys.map { idempotencyKey ->
            CompletableFuture.supplyAsync(
                {
                    startGate.await()
                    transferCreateUseCase.transfer(
                        memberId,
                        idempotencyKey,
                        TestTransferRequestProps.transfer(fromAccountId, toAccountId, amount),
                    )
                },
                executor,
            )
        }

        startGate.countDown()
        return tasks.map { it.join() }
    }

    private fun reset() {
        transactionTemplate.executeWithoutResult {
            entityManager.createQuery("delete from LedgerEntity").executeUpdate()
            entityManager.createQuery("delete from OutboxEventEntity").executeUpdate()
            entityManager.createQuery("delete from TransferEntity").executeUpdate()
            entityManager.createQuery("delete from IdempotencyKeyEntity").executeUpdate()
            entityManager.createQuery("delete from DailyLimitUsageEntity").executeUpdate()
            entityManager.createQuery("delete from ProcessedEventEntity").executeUpdate()
            entityManager.createQuery("delete from AccountEntity").executeUpdate()
            entityManager.createQuery("delete from MemberEntity").executeUpdate()
            entityManager.flush()
            entityManager.clear()
        }
    }

    private fun loadAccountBalance(accountId: Long): BigDecimal = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(
            """
                select a.balance
                  from AccountEntity a
                 where a.id = :accountId
            """.trimIndent(),
            BigDecimal::class.java,
        )
            .setParameter("accountId", accountId)
            .singleResult
    } ?: throw IllegalStateException("Account balance not found")

    private fun countTransfers(): Long = count(
        "select count(t) from TransferEntity t",
    )

    private fun countLedgers(): Long = count(
        "select count(l) from LedgerEntity l",
    )

    private fun countOutboxEvents(): Long = count(
        "select count(o) from OutboxEventEntity o",
    )

    private fun countSentOutboxEvents(): Long = countByOutboxStatus(
        OutboxEventProps.OutboxEventStatusValue.SENT,
    )

    private fun countNewOutboxEvents(): Long = countByOutboxStatus(
        OutboxEventProps.OutboxEventStatusValue.NEW,
    )

    private fun countProcessedEvents(): Long = count(
        "select count(p) from ProcessedEventEntity p",
    )

    private fun count(query: String): Long = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(query, java.lang.Long::class.java)
            .singleResult
            .toLong()
    } ?: 0L

    private fun countByOutboxStatus(
        status: OutboxEventProps.OutboxEventStatusValue,
    ): Long = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(
            """
                select count(o)
                  from OutboxEventEntity o
                 where o.status = :status
            """.trimIndent(),
            java.lang.Long::class.java,
        )
            .setParameter("status", status)
            .singleResult
            .toLong()
    } ?: 0L

    private fun feeFor(amount: BigDecimal): BigDecimal = amount.multiply(TRANSFER_FEE_RATE)
        .setScale(2, RoundingMode.DOWN)

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

    private data class ConcurrentTransferScenario(
        val requestCount: Int,
        val transferAmount: BigDecimal,
        val transferFee: BigDecimal,
        val senderInitialBalance: BigDecimal,
        val sender: MemberSeed,
        val receiver: MemberSeed,
        val senderAccount: AccountSeed,
        val receiverAccount: AccountSeed,
        val transferCountBefore: Long,
        val ledgerCountBefore: Long,
        val outboxCountBefore: Long,
        val sentOutboxCountBefore: Long,
        val processedEventCountBefore: Long,
        val idempotencyKeys: List<String>,
    )

    private data class MemberSeed(
        val memberId: Long,
    )

    private data class AccountSeed(
        val accountId: Long,
    )

    private data class TestMemberProps(
        override val name: String,
        override val email: String,
        override val password: String,
    ) : MemberProps

    private data class TestAccountProps(
        override val memberId: Long?,
        override val bankCode: String,
        override val accountNumber: String,
        override val accountName: String,
        override val balance: BigDecimal,
    ) : AccountProps

    private data class TestIdempotencyKeyCreateProps(
        override val memberId: Long,
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue,
    ) : IdempotencyKeyCreateProps

    private data class TestTransferRequestProps(
        override val fromAccountId: Long,
        override val toAccountId: Long,
        override val amount: BigDecimal,
        override val scope: TransferProps.TransferScopeValue,
        override val fee: BigDecimal,
    ) : TransferRequestProps {
        companion object {
            fun transfer(
                fromAccountId: Long,
                toAccountId: Long,
                amount: BigDecimal,
            ): TestTransferRequestProps = TestTransferRequestProps(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                scope = TransferProps.TransferScopeValue.TRANSFER,
                fee = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.DOWN),
            )

            fun deposit(
                accountId: Long,
                amount: BigDecimal,
            ): TestTransferRequestProps = TestTransferRequestProps(
                fromAccountId = accountId,
                toAccountId = accountId,
                amount = amount,
                scope = TransferProps.TransferScopeValue.DEPOSIT,
                fee = BigDecimal.ZERO,
            )
        }
    }

    private companion object {
        val TRANSFER_FEE_RATE: BigDecimal = BigDecimal("0.01")
        val notificationPayloadTypeReference = object : TypeReference<Map<String, Any?>>() {}
    }
}
