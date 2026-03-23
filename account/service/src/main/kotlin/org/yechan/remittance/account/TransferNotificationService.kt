package org.yechan.remittance.account

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class TransferNotificationService(
    private val accountRepository: AccountRepository,
    private val processedEventRepository: ProcessedEventRepository,
    private val notificationPushPort: NotificationPushPort,
) : TransferNotificationUseCase {
    override fun notify(props: TransferNotificationProps) {
        log.info {
            "transfer.notification.start eventId=${props.eventId} transferId=${props.transferId}"
        }
        if (processedEventRepository.existsByEventId(props.eventId)) {
            log.info { "transfer.notification.duplicate eventId=${props.eventId}" }
            return
        }

        val memberId =
            accountRepository.findById(AccountId(props.toAccountId))
                ?.memberId
                ?: run {
                    log.warn {
                        "transfer.notification.account_not_found toAccountId=${props.toAccountId}"
                    }
                    throw AccountNotFoundException("Account not found")
                }

        val message =
            TransferNotificationMessage(
                MESSAGE_TYPE,
                props.transferId,
                props.amount,
                props.fromAccountId,
                props.occurredAt,
            )

        try {
            log.info {
                "transfer.notification.push memberId=$memberId transferId=${props.transferId}"
            }
            notificationPushPort.push(requireNotNull(memberId), message)
        } catch (ex: RuntimeException) {
            log.error(ex) {
                "transfer.notification.push_failed memberId=$memberId transferId=${props.transferId}"
            }
        }

        processedEventRepository.markProcessed(props.eventId, LocalDateTime.now())
        log.info {
            "transfer.notification.processed eventId=${props.eventId} transferId=${props.transferId}"
        }
    }

    private data class AccountId(
        override val accountId: Long?,
    ) : AccountIdentifier

    private companion object {
        const val MESSAGE_TYPE = "TRANSFER_RECEIVED"
    }
}
