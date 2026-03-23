package org.yechan.remittance.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class TransferNotificationServiceTest {
    @Test
    fun `알림 전송이 성공하면 push하고 processed 상태를 기록한다`() {
        val accountRepository = TestAccountRepository(sampleAccount())
        val processedRepository = TestProcessedEventRepository(false)
        val pushPort = TestNotificationPushPort()
        val service = TransferNotificationService(accountRepository, processedRepository, pushPort)

        service.notify(sampleProps())

        assertEquals(1, pushPort.pushCount.get())
        assertNotNull(processedRepository.markedEventId.get())
    }

    @Test
    fun `이미 처리한 이벤트면 push를 건너뛴다`() {
        val accountRepository = TestAccountRepository(sampleAccount())
        val processedRepository = TestProcessedEventRepository(true)
        val pushPort = TestNotificationPushPort()
        val service = TransferNotificationService(accountRepository, processedRepository, pushPort)

        service.notify(sampleProps())

        assertEquals(0, pushPort.pushCount.get())
    }

    @Test
    fun `push가 실패해도 processed 상태는 기록한다`() {
        val accountRepository = TestAccountRepository(sampleAccount())
        val processedRepository = TestProcessedEventRepository(false)
        val pushPort = TestNotificationPushPort().apply { fail = true }
        val service = TransferNotificationService(accountRepository, processedRepository, pushPort)

        service.notify(sampleProps())

        assertNotNull(processedRepository.markedEventId.get())
    }

    private fun sampleAccount(): AccountModel = Account(
        10L,
        99L,
        "090",
        "123-456",
        "sample-account",
        BigDecimal.ZERO,
    )

    private fun sampleProps(): TransferNotificationProps = TestTransferNotificationProps(
        1L,
        11L,
        10L,
        1L,
        BigDecimal.valueOf(10_000L),
        LocalDateTime.of(2025, 1, 1, 0, 0),
    )

    private data class TestTransferNotificationProps(
        override val eventId: Long,
        override val transferId: Long,
        override val toAccountId: Long,
        override val fromAccountId: Long,
        override val amount: BigDecimal,
        override val occurredAt: LocalDateTime,
    ) : TransferNotificationProps

    private class TestAccountRepository(
        private val account: AccountModel?,
    ) : AccountRepository {
        override fun save(props: AccountProps): AccountModel = throw UnsupportedOperationException("Not used")

        override fun findById(identifier: AccountIdentifier): AccountModel? = account

        override fun findByIdForUpdate(identifier: AccountIdentifier): AccountModel? = throw UnsupportedOperationException("Not used")

        override fun findByMemberIdAndBankCodeAndAccountNumber(
            memberId: Long?,
            bankCode: String,
            accountNumber: String,
        ): AccountModel? = throw UnsupportedOperationException("Not used")

        override fun delete(identifier: AccountIdentifier): Unit = throw UnsupportedOperationException("Not used")
    }

    private class TestProcessedEventRepository(
        private val processed: Boolean,
    ) : ProcessedEventRepository {
        val markedEventId = AtomicReference<Long>()

        override fun existsByEventId(eventId: Long): Boolean = processed

        override fun markProcessed(
            eventId: Long,
            processedAt: LocalDateTime,
        ) {
            markedEventId.set(eventId)
        }
    }

    private class TestNotificationPushPort : NotificationPushPort {
        val pushCount = AtomicInteger()
        var fail: Boolean = false

        override fun push(
            memberId: Long,
            message: TransferNotificationMessage,
        ): Boolean {
            if (fail) {
                throw IllegalStateException("fail")
            }
            pushCount.incrementAndGet()
            return true
        }
    }
}
