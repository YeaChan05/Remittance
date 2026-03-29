package org.yechan.remittance.api.transfer

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.yechan.remittance.account.NotificationSessionRegistry
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

@TestConfiguration
class RecordingNotificationTestConfig {
    @Bean
    fun recordingNotificationStore(): RecordingNotificationStore = RecordingNotificationStore()

    @Bean
    @Primary
    fun recordingNotificationSessionRegistry(
        store: RecordingNotificationStore,
    ): NotificationSessionRegistry = NotificationSessionRegistry {
        RecordingEmitter(store)
    }
}

class RecordingNotificationStore {
    private val sentCount = AtomicInteger()
    private val sentPayloads = ConcurrentLinkedQueue<RecordedPayload>()

    fun sentCount(): Int = sentCount.get()

    fun sentPayloads(): List<RecordedPayload> = sentPayloads.toList()

    fun clear() {
        sentCount.set(0)
        sentPayloads.clear()
    }

    internal fun record(payload: Any) {
        sentPayloads.add(RecordedPayload(payload))
        sentCount.incrementAndGet()
    }

    data class RecordedPayload(
        val payload: Any,
    )
}

private class RecordingEmitter(
    private val store: RecordingNotificationStore,
) : SseEmitter() {
    override fun send(`object`: Any) {
        store.record(`object`)
    }
}
