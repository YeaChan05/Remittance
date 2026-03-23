package org.yechan.remittance.transfer.dto

import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransferQueryResponse(
    val transfers: List<TransferItem>,
) {
    companion object {
        fun from(transfers: List<TransferModel>): TransferQueryResponse = TransferQueryResponse(transfers.map(TransferItem::from))
    }

    data class TransferItem(
        val transferId: Long?,
        val fromAccountId: Long,
        val toAccountId: Long,
        val amount: BigDecimal,
        val scope: TransferProps.TransferScopeValue,
        val status: TransferProps.TransferStatusValue,
        val requestedAt: LocalDateTime,
        val completedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(transfer: TransferModel): TransferItem = TransferItem(
                transfer.transferId,
                transfer.fromAccountId,
                transfer.toAccountId,
                transfer.amount,
                transfer.scope,
                transfer.status,
                transfer.requestedAt,
                transfer.completedAt,
            )
        }
    }
}
