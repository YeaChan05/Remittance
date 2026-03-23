package org.yechan.remittance.account

class NotificationPushAdapter(
    private val registry: NotificationSessionRegistry,
) : NotificationPushPort {
    override fun push(
        memberId: Long,
        message: TransferNotificationMessage,
    ): Boolean = registry.push(memberId, message)
}
