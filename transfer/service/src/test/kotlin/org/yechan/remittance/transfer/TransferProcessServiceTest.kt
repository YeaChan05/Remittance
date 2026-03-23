package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.yechan.remittance.account.AccountIdentifier
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class TransferProcessServiceTest {
    @Test
    fun `일반 이체 성공 시 잔액 변경과 outbox 생성을 수행한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
        )

        val result = fixture.service.process(10L, "idem-key", TestTransferRequestProps(), now())

        assertThat(result.transferId).isEqualTo(1L)
        assertThat(fixture.accountClient.lockCallCount).isEqualTo(1)
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isEqualTo(1)
        assertThat(fixture.accountClient.appliedBalanceChange).isEqualTo(
            TransferBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 2L,
                fromBalance = BigDecimal("899"),
                toBalance = BigDecimal("300"),
            ),
        )
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isEqualTo(1)
        assertThat(fixture.dailyLimitUsageRepository.usage.updateCallCount).isEqualTo(1)
        assertThat(fixture.transferRepository.saveCallCount).isEqualTo(1)
        assertThat(fixture.outboxEventRepository.saveCallCount).isEqualTo(1)
        assertThat(fixture.outboxEventRepository.savedAggregateIds).containsExactly("1")
        assertThat(fixture.idempotencyKeyRepository.markSucceededCallCount).isEqualTo(1)
        assertThat(fixture.idempotencyKeyRepository.markedSucceededSnapshot).contains(
            "SUCCEEDED",
            "\"transferId\":1",
        )
    }

    @Test
    fun `입금 성공 시 동일 계좌 잔액만 증가시키고 outbox를 생성하지 않는다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
            ),
        )

        val result = fixture.service.process(
            10L,
            "idem-key",
            TestTransferRequestProps(
                fromAccountId = 1L,
                toAccountId = 1L,
                scope = TransferProps.TransferScopeValue.DEPOSIT,
                fee = BigDecimal.ZERO,
            ),
            now(),
        )

        assertThat(result.transferId).isEqualTo(1L)
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isZero()
        assertThat(fixture.accountClient.appliedBalanceChange).isEqualTo(
            TransferBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 1L,
                fromBalance = BigDecimal("1100"),
                toBalance = BigDecimal("1100"),
            ),
        )
        assertThat(fixture.outboxEventRepository.saveCallCount).isZero()
        assertThat(fixture.idempotencyKeyRepository.markSucceededCallCount).isEqualTo(1)
    }

    @Test
    fun `출금 성공 시 출금 계좌 잔액만 감소시키고 outbox를 생성하지 않는다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
            ),
        )

        val result = fixture.service.process(
            10L,
            "idem-key",
            TestTransferRequestProps(
                fromAccountId = 1L,
                toAccountId = 1L,
                scope = TransferProps.TransferScopeValue.WITHDRAW,
                fee = BigDecimal.ZERO,
            ),
            now(),
        )

        assertThat(result.transferId).isEqualTo(1L)
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isEqualTo(1)
        assertThat(fixture.accountClient.appliedBalanceChange).isEqualTo(
            TransferBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 1L,
                fromBalance = BigDecimal("900"),
                toBalance = BigDecimal("900"),
            ),
        )
        assertThat(fixture.outboxEventRepository.saveCallCount).isZero()
        assertThat(fixture.idempotencyKeyRepository.markSucceededCallCount).isEqualTo(1)
    }

    @Test
    fun `같은 계좌로 이체하면 INVALID_REQUEST를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
            ),
        )

        assertThatThrownBy {
            fixture.service.process(
                10L,
                "idem-key",
                TestTransferRequestProps(fromAccountId = 1L, toAccountId = 1L),
                now(),
            )
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INVALID_REQUEST)

        assertThat(fixture.accountClient.lockCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
        assertThat(fixture.idempotencyKeyRepository.markSucceededCallCount).isZero()
    }

    @Test
    fun `계좌 잠금에 실패하면 ACCOUNT_NOT_FOUND를 반환한다`() {
        val fixture = transferFixture(lockedAccounts = null)

        assertThatThrownBy {
            fixture.service.process(10L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.ACCOUNT_NOT_FOUND)

        assertThat(fixture.accountClient.lockCallCount).isEqualTo(1)
        assertThat(fixture.memberClient.existsCallIds).isEmpty()
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
        assertThat(fixture.outboxEventRepository.saveCallCount).isZero()
        assertThat(fixture.idempotencyKeyRepository.markSucceededCallCount).isZero()
    }

    @Test
    fun `송금 계좌 회원이 없으면 OWNER_NOT_FOUND를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
            existingMemberIds = setOf(20L),
        )

        assertThatThrownBy {
            fixture.service.process(10L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.OWNER_NOT_FOUND)

        assertThat(fixture.memberClient.existsCallIds).containsExactly(10L)
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isZero()
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
    }

    @Test
    fun `수취인 회원이 없으면 MEMBER_NOT_FOUND를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
            existingMemberIds = setOf(10L),
        )

        assertThatThrownBy {
            fixture.service.process(10L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.MEMBER_NOT_FOUND)

        assertThat(fixture.memberClient.existsCallIds).containsExactly(10L, 20L)
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isZero()
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
    }

    @Test
    fun `요청 사용자와 소유자가 다르면 INVALID_REQUEST를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
            existingMemberIds = setOf(10L, 20L),
        )

        assertThatThrownBy {
            fixture.service.process(99L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INVALID_REQUEST)

        assertThat(fixture.memberClient.existsCallIds).containsExactly(10L, 20L)
        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isZero()
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
    }

    @Test
    fun `일일 한도 초과 시 DAILY_LIMIT_EXCEEDED를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
            initialUsedAmount = BigDecimal.valueOf(3_000_000),
        )

        assertThatThrownBy {
            fixture.service.process(10L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.DAILY_LIMIT_EXCEEDED)

        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isEqualTo(1)
        assertThat(fixture.dailyLimitUsageRepository.usage.updateCallCount).isZero()
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
        assertThat(fixture.outboxEventRepository.saveCallCount).isZero()
    }

    @Test
    fun `잔액이 부족하면 INSUFFICIENT_BALANCE를 반환한다`() {
        val fixture = transferFixture(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("50")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
        )

        assertThatThrownBy {
            fixture.service.process(
                10L,
                "idem-key",
                TestTransferRequestProps(amount = BigDecimal("100")),
                now(),
            )
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INSUFFICIENT_BALANCE)

        assertThat(fixture.dailyLimitUsageRepository.findOrCreateCallCount).isEqualTo(1)
        assertThat(fixture.dailyLimitUsageRepository.usage.updateCallCount).isEqualTo(1)
        assertThat(fixture.accountClient.applyBalanceChangeCallCount).isZero()
        assertThat(fixture.transferRepository.saveCallCount).isZero()
        assertThat(fixture.outboxEventRepository.saveCallCount).isZero()
    }

    private fun transferFixture(
        lockedAccounts: TransferLockedAccounts?,
        existingMemberIds: Set<Long> = setOf(10L, 20L),
        initialUsedAmount: BigDecimal = BigDecimal.ZERO,
    ): TransferFixture {
        val accountClient = FakeTransferAccountClient(lockedAccounts = lockedAccounts)
        val transferRepository = FakeTransferRepository()
        val outboxEventRepository = FakeOutboxEventRepository()
        val idempotencyKeyRepository = FakeIdempotencyKeyRepository()
        val dailyLimitUsageRepository = FakeDailyLimitUsageRepository(initialUsedAmount)
        val memberClient = FakeTransferMemberClient(existingMemberIds)
        val service = TransferProcessService(
            accountClient,
            transferRepository,
            outboxEventRepository,
            idempotencyKeyRepository,
            dailyLimitUsageRepository,
            memberClient,
            TransferSnapshotUtil(ObjectMapper()),
        )
        return TransferFixture(
            service,
            accountClient,
            transferRepository,
            outboxEventRepository,
            idempotencyKeyRepository,
            dailyLimitUsageRepository,
            memberClient,
        )
    }

    private fun now(): LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

    private data class TransferFixture(
        val service: TransferProcessService,
        val accountClient: FakeTransferAccountClient,
        val transferRepository: FakeTransferRepository,
        val outboxEventRepository: FakeOutboxEventRepository,
        val idempotencyKeyRepository: FakeIdempotencyKeyRepository,
        val dailyLimitUsageRepository: FakeDailyLimitUsageRepository,
        val memberClient: FakeTransferMemberClient,
    )

    private data class TestTransferRequestProps(
        override val fromAccountId: Long = 1L,
        override val toAccountId: Long = 2L,
        override val amount: BigDecimal = BigDecimal("100"),
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER,
        override val fee: BigDecimal = BigDecimal("1"),
    ) : TransferRequestProps

    private class FakeTransferAccountClient(
        private val lockedAccounts: TransferLockedAccounts?,
        private val accounts: Map<Long, TransferAccountSnapshot> = emptyMap(),
    ) : TransferAccountClient {
        var appliedBalanceChange: TransferBalanceChangeCommand? = null
        var lockCallCount: Int = 0
        var applyBalanceChangeCallCount: Int = 0

        override fun get(accountId: Long): TransferAccountSnapshot? = accounts[accountId]

        override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? {
            lockCallCount += 1
            return lockedAccounts
        }

        override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
            applyBalanceChangeCallCount += 1
            appliedBalanceChange = command
        }
    }

    private class FakeTransferMemberClient(
        existingMemberIds: Set<Long>,
    ) : TransferMemberClient {
        private val existing = existingMemberIds.toSet()
        val existsCallIds = mutableListOf<Long>()

        override fun exists(memberId: Long): Boolean {
            existsCallIds += memberId
            return existing.contains(memberId)
        }
    }

    private class FakeTransferRepository : TransferRepository {
        var saveCallCount: Int = 0
        var savedProps: TransferRequestProps? = null

        override fun save(props: TransferRequestProps): TransferModel {
            saveCallCount += 1
            savedProps = props
            return Transfer(
                transferId = 1L,
                fromAccountId = props.fromAccountId,
                toAccountId = props.toAccountId,
                amount = props.amount,
                scope = props.scope,
                status = TransferProps.TransferStatusValue.SUCCEEDED,
                requestedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                completedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            )
        }

        override fun findById(identifier: TransferIdentifier): TransferModel? = null

        override fun findCompletedByAccountId(
            identifier: AccountIdentifier,
            condition: TransferQueryCondition,
        ): List<TransferModel> = emptyList()

        override fun sumAmountByFromAccountIdAndScopeBetween(
            identifier: AccountIdentifier,
            scope: TransferProps.TransferScopeValue,
            from: LocalDateTime,
            to: LocalDateTime,
        ): BigDecimal = BigDecimal.ZERO
    }

    private class FakeOutboxEventRepository : OutboxEventRepository {
        val savedAggregateIds = mutableListOf<String>()
        var saveCallCount: Int = 0

        override fun save(props: OutboxEventProps): OutboxEventModel {
            saveCallCount += 1
            savedAggregateIds += props.aggregateId
            return OutboxEvent(
                1L,
                props.aggregateType,
                props.aggregateId,
                props.eventType,
                props.payload,
                props.status,
                LocalDateTime.of(2026, 1, 1, 0, 0),
            )
        }

        override fun findNewForPublish(limit: Int?): List<OutboxEventModel> = emptyList()

        override fun markSent(identifier: OutboxEventIdentifier) = Unit
    }

    private class FakeIdempotencyKeyRepository : IdempotencyKeyRepository {
        var markedSucceededSnapshot: String = ""
        var markSucceededCallCount: Int = 0

        override fun save(props: IdempotencyKeyProps): IdempotencyKeyModel = throw UnsupportedOperationException()

        override fun findByKey(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
        ): IdempotencyKeyModel? = null

        override fun tryMarkInProgress(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            requestHash: String,
            startedAt: LocalDateTime,
        ): Boolean = false

        override fun markSucceeded(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel {
            markSucceededCallCount += 1
            markedSucceededSnapshot = responseSnapshot
            return IdempotencyKey(
                1L,
                memberId,
                idempotencyKey,
                LocalDateTime.of(2026, 1, 2, 0, 0),
                scope,
                IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED,
                null,
                responseSnapshot,
                null,
                completedAt,
            )
        }

        override fun markFailed(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel = throw UnsupportedOperationException()

        override fun markTimeoutBefore(
            cutoff: LocalDateTime,
            responseSnapshot: String,
        ): Int = 0
    }

    private class FakeDailyLimitUsageRepository(
        initialUsedAmount: BigDecimal,
    ) : DailyLimitUsageRepository {
        val usage = TestDailyLimitUsage(initialUsedAmount)
        var findOrCreateCallCount: Int = 0

        override fun findOrCreateForUpdate(
            identifier: AccountIdentifier,
            scope: TransferProps.TransferScopeValue,
            usageDate: LocalDate,
        ): DailyLimitUsageModel {
            findOrCreateCallCount += 1
            return usage
        }
    }

    private class TestDailyLimitUsage(
        override var usedAmount: BigDecimal,
    ) : DailyLimitUsageModel {
        override val accountId: Long = 1L
        override val scope: TransferProps.TransferScopeValue =
            TransferProps.TransferScopeValue.TRANSFER
        override val usageDate: LocalDate = LocalDate.of(2026, 1, 1)
        override val dailyLimitUsageId: Long? = 1L
        var updateCallCount: Int = 0

        override fun updateUsedAmount(usedAmount: BigDecimal) {
            updateCallCount += 1
            this.usedAmount = usedAmount
        }
    }
}
