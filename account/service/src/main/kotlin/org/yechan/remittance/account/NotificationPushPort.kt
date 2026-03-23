package org.yechan.remittance.account

fun interface NotificationPushPort {
    fun push(
        memberId: Long,
        message: TransferNotificationMessage,
    ): Boolean
}
