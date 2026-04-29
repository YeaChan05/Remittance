package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.yechan.remittance.Money
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

open class LedgerWriter(
    private val ledgerRepository: LedgerRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun record(
        props: TransferRequestProps,
        result: TransferResult,
        now: LocalDateTime,
    ) {
        if (result.transferId == null) {
            log.info { "ledger.record.skip transferId=null" }
            return
        }
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            log.info { "ledger.record.deposit transferId=${result.transferId}" }
            saveLedgerIfAbsent(
                result.transferId,
                props.toAccountId,
                props.amount,
                LedgerProps.LedgerSideValue.CREDIT,
                now,
            )
            return
        }

        val debitAmount = props.debit()
        log.info {
            "ledger.record.debit transferId=${result.transferId} fromAccountId=${props.fromAccountId}"
        }
        saveLedgerIfAbsent(
            result.transferId,
            props.fromAccountId,
            debitAmount,
            LedgerProps.LedgerSideValue.DEBIT,
            now,
        )

        if (props.scope == TransferProps.TransferScopeValue.TRANSFER) {
            log.info {
                "ledger.record.credit transferId=${result.transferId} toAccountId=${props.toAccountId}"
            }
            saveLedgerIfAbsent(
                result.transferId,
                props.toAccountId,
                props.amount,
                LedgerProps.LedgerSideValue.CREDIT,
                now,
            )
        }
    }

    private fun saveLedgerIfAbsent(
        transferId: Long?,
        accountId: Long,
        amount: Money,
        side: LedgerProps.LedgerSideValue,
        now: LocalDateTime,
    ) {
        val resolvedTransferId = requireNotNull(transferId)
        log.info { "ledger.record.save transferId=$transferId accountId=$accountId side=$side" }
        val saved = ledgerRepository.saveIfAbsent(
            LedgerCreateCommand(
                resolvedTransferId,
                accountId,
                amount,
                side,
                now,
            ),
        )
        if (!saved) {
            log.debug { "ledger.record.exists transferId=$transferId accountId=$accountId side=$side" }
        }
    }

    private data class LedgerCreateCommand(
        override val transferId: Long,
        override val accountId: Long,
        override val amount: Money,
        override val side: LedgerProps.LedgerSideValue,
        override val createdAt: LocalDateTime?,
    ) : LedgerProps
}
