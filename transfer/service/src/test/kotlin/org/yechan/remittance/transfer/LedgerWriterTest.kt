package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class LedgerWriterTest {
    @Test
    fun `transferId가 없으면 저장을 건너뛴다`() {
        val repository = FakeLedgerRepository()
        val writer = LedgerWriter(repository)

        writer.record(TestTransferRequestProps(), TransferResult.inProgress(), now())

        assertThat(repository.existsCalls).isEmpty()
        assertThat(repository.savedProps).isEmpty()
    }

    @Test
    fun `입금은 CREDIT 1건만 저장한다`() {
        val repository = FakeLedgerRepository()
        val writer = LedgerWriter(repository)

        writer.record(
            TestTransferRequestProps(
                fromAccountId = 1L,
                toAccountId = 1L,
                amount = BigDecimal("100"),
                scope = TransferProps.TransferScopeValue.DEPOSIT,
                fee = BigDecimal.ZERO,
            ),
            TransferResult.succeeded(1L),
            now(),
        )

        assertThat(repository.savedProps).hasSize(1)
        assertThat(repository.savedProps.first().accountId).isEqualTo(1L)
        assertThat(repository.savedProps.first().side).isEqualTo(LedgerProps.LedgerSideValue.CREDIT)
        assertThat(repository.savedProps.first().amount).isEqualByComparingTo("100")
    }

    @Test
    fun `출금은 DEBIT 1건만 저장한다`() {
        val repository = FakeLedgerRepository()
        val writer = LedgerWriter(repository)

        writer.record(
            TestTransferRequestProps(
                amount = BigDecimal("100"),
                scope = TransferProps.TransferScopeValue.WITHDRAW,
                fee = BigDecimal.ZERO,
            ),
            TransferResult.succeeded(1L),
            now(),
        )

        assertThat(repository.savedProps).hasSize(1)
        assertThat(repository.savedProps.first().accountId).isEqualTo(1L)
        assertThat(repository.savedProps.first().side).isEqualTo(LedgerProps.LedgerSideValue.DEBIT)
        assertThat(repository.savedProps.first().amount).isEqualByComparingTo("100")
    }

    @Test
    fun `일반 이체는 DEBIT CREDIT 2건을 저장한다`() {
        val repository = FakeLedgerRepository()
        val writer = LedgerWriter(repository)

        writer.record(
            TestTransferRequestProps(
                amount = BigDecimal("100"),
                scope = TransferProps.TransferScopeValue.TRANSFER,
                fee = BigDecimal("1"),
            ),
            TransferResult.succeeded(1L),
            now(),
        )

        assertThat(repository.savedProps).hasSize(2)
        assertThat(repository.savedProps.map { it.side }).containsExactly(
            LedgerProps.LedgerSideValue.DEBIT,
            LedgerProps.LedgerSideValue.CREDIT,
        )
        assertThat(repository.savedProps.map { it.amount }).containsExactly(
            BigDecimal("101"),
            BigDecimal("100"),
        )
    }

    @Test
    fun `이미 같은 ledger가 있으면 저장하지 않는다`() {
        val repository = FakeLedgerRepository(
            existingKeys = mutableSetOf(LedgerKey(1L, 1L, LedgerProps.LedgerSideValue.DEBIT)),
        )
        val writer = LedgerWriter(repository)

        writer.record(
            TestTransferRequestProps(
                amount = BigDecimal("100"),
                scope = TransferProps.TransferScopeValue.WITHDRAW,
                fee = BigDecimal.ZERO,
            ),
            TransferResult.succeeded(1L),
            now(),
        )

        assertThat(repository.existsCalls).containsExactly(
            LedgerKey(1L, 1L, LedgerProps.LedgerSideValue.DEBIT),
        )
        assertThat(repository.savedProps).isEmpty()
    }

    private fun now(): LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)

    private data class TestTransferRequestProps(
        override val fromAccountId: Long = 1L,
        override val toAccountId: Long = 2L,
        override val amount: BigDecimal = BigDecimal("100"),
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER,
        override val fee: BigDecimal = BigDecimal("1"),
    ) : TransferRequestProps

    private data class LedgerKey(
        val transferId: Long,
        val accountId: Long,
        val side: LedgerProps.LedgerSideValue,
    )

    private class FakeLedgerRepository(
        private val existingKeys: MutableSet<LedgerKey> = mutableSetOf(),
    ) : LedgerRepository {
        val existsCalls = mutableListOf<LedgerKey>()
        val savedProps = mutableListOf<LedgerProps>()

        override fun save(props: LedgerProps): LedgerModel {
            savedProps += props
            existingKeys += LedgerKey(props.transferId, props.accountId, props.side)
            return object : LedgerModel, LedgerProps by props {
                override val ledgerId: Long? = savedProps.size.toLong()
            }
        }

        override fun existsByTransferIdAndAccountIdAndSide(
            transferId: Long,
            accountId: Long,
            side: LedgerProps.LedgerSideValue,
        ): Boolean {
            val key = LedgerKey(transferId, accountId, side)
            existsCalls += key
            return existingKeys.contains(key)
        }

        override fun sumAmountByAccountIdAndSideBetween(
            accountId: Long,
            side: LedgerProps.LedgerSideValue,
            from: LocalDateTime,
            to: LocalDateTime,
        ): BigDecimal = BigDecimal.ZERO
    }
}
