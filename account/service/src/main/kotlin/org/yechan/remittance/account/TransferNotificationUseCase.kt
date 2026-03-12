package org.yechan.remittance.account

fun interface TransferNotificationUseCase {
    fun notify(props: TransferNotificationProps)
}
