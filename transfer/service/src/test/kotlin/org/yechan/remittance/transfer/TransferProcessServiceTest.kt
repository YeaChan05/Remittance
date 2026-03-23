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
    fun `유효한 송금 요청은 잔액 변경과 이체 저장을 수행한다`() {
        val accountClient = FakeTransferAccountClient(
            lockedAccounts = TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
        )
        val transferRepository = FakeTransferRepository()
        val outboxEventRepository = FakeOutboxEventRepository()
        val idempotencyKeyRepository = FakeIdempotencyKeyRepository()
        val dailyLimitUsageRepository = FakeDailyLimitUsageRepository(BigDecimal.ZERO)
        val memberClient = FakeTransferMemberClient(10L, 20L)
        val service = TransferProcessService(
            accountClient,
            transferRepository,
            outboxEventRepository,
            idempotencyKeyRepository,
            dailyLimitUsageRepository,
            memberClient,
            TransferSnapshotUtil(ObjectMapper()),
        )

        val result = service.process(10L, "idem-key", TestTransferRequestProps(), now())

        assertThat(result.transferId).isEqualTo(1L)
        assertThat(accountClient.appliedBalanceChange).isEqualTo(
            TransferBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 2L,
                fromBalance = BigDecimal("899"),
                toBalance = BigDecimal("300"),
            ),
        )
        assertThat(outboxEventRepository.savedAggregateIds).containsExactly("1")
        assertThat(idempotencyKeyRepository.markedSucceededSnapshot).contains("SUCCEEDED", "\"transferId\":1")
    }

    @Test
    fun `같은 계좌로 이체하면 INVALID_REQUEST를 반환한다`() {
        val service = serviceWithLockedAccounts(
            TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
            ),
        )

        assertThatThrownBy {
            service.process(
                10L,
                "idem-key",
                TestTransferRequestProps(fromAccountId = 1L, toAccountId = 1L),
                now(),
            )
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INVALID_REQUEST)
    }

    @Test
    fun `송금 계좌 회원이 없으면 OWNER_NOT_FOUND를 반환한다`() {
        val service = TransferProcessService(
            FakeTransferAccountClient(
                lockedAccounts = TransferLockedAccounts(
                    TransferAccountSnapshot(1L, 10L, BigDecimal("1000")),
                    TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
                ),
            ),
            FakeTransferRepository(),
            FakeOutboxEventRepository(),
            FakeIdempotencyKeyRepository(),
            FakeDailyLimitUsageRepository(BigDecimal.ZERO),
            FakeTransferMemberClient(20L),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThatThrownBy {
            service.process(10L, "idem-key", TestTransferRequestProps(), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.OWNER_NOT_FOUND)
    }

    @Test
    fun `잔액이 부족하면 INSUFFICIENT_BALANCE를 반환한다`() {
        val service = serviceWithLockedAccounts(
            TransferLockedAccounts(
                TransferAccountSnapshot(1L, 10L, BigDecimal("50")),
                TransferAccountSnapshot(2L, 20L, BigDecimal("200")),
            ),
        )

        assertThatThrownBy {
            service.process(10L, "idem-key", TestTransferRequestProps(amount = BigDecimal("100")), now())
        }.isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INSUFFICIENT_BALANCE)
    }

    private fun serviceWithLockedAccounts(lockedAccounts: TransferLockedAccounts): TransferProcessService = TransferProcessService(
        FakeTransferAccountClient(lockedAccounts = lockedAccounts),
        FakeTransferRepository(),
        FakeOutboxEventRepository(),
        FakeIdempotencyKeyRepository(),
        FakeDailyLimitUsageRepository(BigDecimal.ZERO),
        FakeTransferMemberClient(10L, 20L),
        TransferSnapshotUtil(ObjectMapper()),
    )

    private fun now(): LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

    private data class TestTransferRequestProps(
        override val fromAccountId: Long = 1L,
        override val toAccountId: Long = 2L,
        override val amount: BigDecimal = BigDecimal("100"),
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER,
        override val fee: BigDecimal = BigDecimal("1"),
    ) : TransferRequestProps

    private class FakeTransferAccountClient(
        private val lockedAccounts: TransferLockedAccounts? = null,
        private val accounts: Map<Long, TransferAccountSnapshot> = emptyMap(),
    ) : TransferAccountClient {
        var appliedBalanceChange: TransferBalanceChangeCommand? = null

        override fun get(accountId: Long): TransferAccountSnapshot? = accounts[accountId]

        override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = lockedAccounts

        override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
            appliedBalanceChange = command
        }
    }

    private class FakeTransferMemberClient(
        vararg existingMemberIds: Long,
    ) : TransferMemberClient {
        private val existing = existingMemberIds.toSet()

        override fun exists(memberId: Long): Boolean = existing.contains(memberId)
    }

    private class FakeTransferRepository : TransferRepository {
        override fun save(props: TransferRequestProps): TransferModel = Transfer(
            transferId = 1L,
            fromAccountId = props.fromAccountId,
            toAccountId = props.toAccountId,
            amount = props.amount,
            scope = props.scope,
            status = TransferProps.TransferStatusValue.SUCCEEDED,
            requestedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            completedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

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

        override fun save(props: OutboxEventProps): OutboxEventModel {
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
        private val usage = TestDailyLimitUsage(initialUsedAmount)

        override fun findOrCreateForUpdate(
            identifier: AccountIdentifier,
            scope: TransferProps.TransferScopeValue,
            usageDate: LocalDate,
        ): DailyLimitUsageModel = usage
    }

    private class TestDailyLimitUsage(
        override var usedAmount: BigDecimal,
    ) : DailyLimitUsageModel {
        override val accountId: Long = 1L
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER
        override val usageDate: LocalDate = LocalDate.of(2026, 1, 1)
        override val dailyLimitUsageId: Long? = 1L

        override fun updateUsedAmount(usedAmount: BigDecimal) {
            this.usedAmount = usedAmount
        }
    }
}
