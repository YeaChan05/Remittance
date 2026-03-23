package org.yechan.remittance.transfer

data class TransferResult(
    val status: TransferProps.TransferStatusValue,
    val transferId: Long?,
    val errorCode: String?,
) {
    companion object {
        fun inProgress(): TransferResult = TransferResult(TransferProps.TransferStatusValue.IN_PROGRESS, null, null)

        fun succeeded(transferId: Long): TransferResult = TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, transferId, null)

        fun failed(errorCode: TransferFailureCode): TransferResult = TransferResult(TransferProps.TransferStatusValue.FAILED, null, errorCode.name)
    }
}
