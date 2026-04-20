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
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [AggregateApplication::class],
    properties = ["transfer.outbox.publisher.enabled=false"],
)
@Import(RecordingNotificationTestConfig::class)
class AggregateTransferFullFlowSpecs {
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
    fun `멱등키 발급부터 송금 성공, outbox 발행, 알림 전달까지 full flow를 검증한다`() {
        val receiver = createMember("receiver")
        val sender = createMember("sender")
        val receiverAccount = createAccount(receiver.memberId, "receiver-main")
        val senderAccount = createAccount(sender.memberId, "sender-main")
        val initialBalance = BigDecimal.valueOf(100_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)
        val transferFee = feeFor(transferAmount)

        val initialDeposit = transferCreateUseCase.transfer(
            sender.memberId,
            issueIdempotencyKey(sender.memberId, IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT),
            TestTransferRequestProps.deposit(senderAccount.accountId, initialBalance),
        )
        assertThat(initialDeposit.status).isEqualTo(TransferProps.TransferStatusValue.SUCCEEDED)

        notificationSubscriptionHandler.subscribe(receiver.memberId)

        val transferCountBefore = countTransfers()
        val ledgerCountBefore = countLedgers()
        val outboxCountBefore = countOutboxEvents()
        val processedEventCountBefore = countProcessedEvents()

        val idempotencyKey = issueIdempotencyKey(
            sender.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val response = transferCreateUseCase.transfer(
            sender.memberId,
            idempotencyKey,
            TestTransferRequestProps.transfer(
                fromAccountId = senderAccount.accountId,
                toAccountId = receiverAccount.accountId,
                amount = transferAmount,
            ),
        )

        assertThat(response.status).isEqualTo(TransferProps.TransferStatusValue.SUCCEEDED)
        assertThat(response.errorCode).isNull()
        assertThat(response.transferId).isNotNull()
        assertThat(loadAccountBalance(senderAccount.accountId)).isEqualByComparingTo(
            initialBalance.subtract(transferAmount).subtract(transferFee),
        )
        assertThat(loadAccountBalance(receiverAccount.accountId)).isEqualByComparingTo(transferAmount)
        assertThat(countTransfers()).isEqualTo(transferCountBefore + 1)
        assertThat(countLedgers()).isEqualTo(ledgerCountBefore + 2)
        assertThat(countOutboxEvents()).isEqualTo(outboxCountBefore + 1)

        val idempotency = loadIdempotencyKey(sender.memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("SUCCEEDED")
        assertThat(idempotency.responseSnapshot).contains("SUCCEEDED")
        assertThat(idempotency.responseSnapshot).contains("\"transferId\":${response.transferId}")

        val outboxBeforePublish = loadOutboxEvents(requireNotNull(response.transferId))
        assertThat(outboxBeforePublish).hasSize(1)
        assertThat(outboxBeforePublish.first().status).isEqualTo("NEW")

        val published = publishAllTransferEvents()
        assertThat(published).isEqualTo(1)

        eventually(Duration.ofSeconds(10), Duration.ofMillis(100)) {
            assertThat(countProcessedEvents()).isEqualTo(processedEventCountBefore + 1)
            assertThat(notificationStore.sentCount()).isEqualTo(1)
        }

        val outboxAfterPublish = loadOutboxEvents(requireNotNull(response.transferId))
        assertThat(outboxAfterPublish).hasSize(1)
        assertThat(outboxAfterPublish.first().status).isEqualTo("SENT")

        val payload = notificationPayloads().single()
        assertThat(payload["type"]).isEqualTo("TRANSFER_RECEIVED")
        assertThat((payload["transferId"] as Number).toLong()).isEqualTo(response.transferId)
        assertThat((payload["fromAccountId"] as Number).toLong()).isEqualTo(senderAccount.accountId)
        assertThat(BigDecimal(payload["amount"].toString())).isEqualByComparingTo(transferAmount)
    }

    @Test
    fun `잔액 부족 실패 시 idempotency는 FAILED로 남고 outbox와 알림은 발생하지 않는다`() {
        val receiver = createMember("receiver")
        val sender = createMember("sender")
        val receiverAccount = createAccount(receiver.memberId, "receiver-main")
        val senderAccount = createAccount(sender.memberId, "sender-main")
        val initialBalance = BigDecimal.valueOf(10_000L)
        val transferAmount = BigDecimal.valueOf(50_000L)

        val initialDeposit = transferCreateUseCase.transfer(
            sender.memberId,
            issueIdempotencyKey(sender.memberId, IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT),
            TestTransferRequestProps.deposit(senderAccount.accountId, initialBalance),
        )
        assertThat(initialDeposit.status).isEqualTo(TransferProps.TransferStatusValue.SUCCEEDED)

        notificationSubscriptionHandler.subscribe(receiver.memberId)

        val transferCountBefore = countTransfers()
        val ledgerCountBefore = countLedgers()
        val outboxCountBefore = countOutboxEvents()
        val processedEventCountBefore = countProcessedEvents()

        val idempotencyKey = issueIdempotencyKey(
            sender.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val response = transferCreateUseCase.transfer(
            sender.memberId,
            idempotencyKey,
            TestTransferRequestProps.transfer(
                fromAccountId = senderAccount.accountId,
                toAccountId = receiverAccount.accountId,
                amount = transferAmount,
            ),
        )

        assertThat(response.status).isEqualTo(TransferProps.TransferStatusValue.FAILED)
        assertThat(response.transferId).isNull()
        assertThat(response.errorCode).isEqualTo("INSUFFICIENT_BALANCE")
        assertThat(loadAccountBalance(senderAccount.accountId)).isEqualByComparingTo(initialBalance)
        assertThat(loadAccountBalance(receiverAccount.accountId)).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(countTransfers()).isEqualTo(transferCountBefore)
        assertThat(countLedgers()).isEqualTo(ledgerCountBefore)
        assertThat(countOutboxEvents()).isEqualTo(outboxCountBefore)

        val idempotency = loadIdempotencyKey(sender.memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("FAILED")
        assertThat(idempotency.responseSnapshot).contains("FAILED")
        assertThat(idempotency.responseSnapshot).contains("INSUFFICIENT_BALANCE")

        val published = publishAllTransferEvents()
        assertThat(published).isZero()
        assertThat(countProcessedEvents()).isEqualTo(processedEventCountBefore)
        assertThat(notificationStore.sentCount()).isZero()
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

    private fun publishAllTransferEvents(): Int {
        var published = 0
        while (true) {
            val batch = transferEventPublishUseCase.publish(null)
            if (batch == 0) {
                return published
            }
            published += batch
        }
    }

    private fun notificationPayloads(): List<Map<String, Any?>> = notificationStore.sentPayloads()
        .map { objectMapper.convertValue(it.payload, notificationPayloadTypeReference) }

    private fun loadIdempotencyKey(
        memberId: Long,
        idempotencyKey: String,
    ): StoredIdempotency = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(
            """
                select i.status, i.responseSnapshot
                  from IdempotencyKeyEntity i
                 where i.memberId = :memberId
                   and i.idempotencyKey = :idempotencyKey
            """.trimIndent(),
            Array<Any>::class.java,
        )
            .setParameter("memberId", memberId)
            .setParameter("idempotencyKey", idempotencyKey)
            .singleResult
            .let { row ->
                StoredIdempotency(
                    status = row[0].toString(),
                    responseSnapshot = row[1].toString(),
                )
            }
    }

    private fun loadOutboxEvents(
        transferId: Long,
    ): List<StoredOutboxEvent> = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(
            """
                select o.status, o.payload
                  from OutboxEventEntity o
                 where o.aggregateId = :aggregateId
                 order by o.aggregateId asc
            """.trimIndent(),
            Array<Any>::class.java,
        )
            .setParameter("aggregateId", transferId.toString())
            .resultList
            .map { row ->
                StoredOutboxEvent(
                    status = row[0].toString(),
                    payload = row[1].toString(),
                )
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

    private fun countTransfers(): Long = count("select count(t) from TransferEntity t")

    private fun countLedgers(): Long = count("select count(l) from LedgerEntity l")

    private fun countOutboxEvents(): Long = count("select count(o) from OutboxEventEntity o")

    private fun countProcessedEvents(): Long = count("select count(p) from ProcessedEventEntity p")

    private fun count(query: String): Long = transactionTemplate.execute {
        entityManager.clear()
        entityManager.createQuery(query, Long::class.java)
            .singleResult
            .toLong()
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

    private fun feeFor(amount: BigDecimal): BigDecimal = amount.multiply(TRANSFER_FEE_RATE)
        .setScale(2, RoundingMode.DOWN)

    private data class MemberSeed(
        val memberId: Long,
    )

    private data class AccountSeed(
        val accountId: Long,
    )

    private data class StoredIdempotency(
        val status: String,
        val responseSnapshot: String?,
    )

    private data class StoredOutboxEvent(
        val status: String,
        val payload: String,
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
