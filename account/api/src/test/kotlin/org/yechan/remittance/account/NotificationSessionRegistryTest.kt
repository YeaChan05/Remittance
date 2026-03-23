package org.yechan.remittance.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class NotificationSessionRegistryTest {
    @Test
    fun `구독 연결은 member id로 emitter를 저장한다`() {
        val registry = NotificationSessionRegistry(::TestEmitter)

        val emitter = registry.connectRegister(1L)

        assertNotNull(emitter)
        assertNotNull(registry.find(1L))
    }

    @Test
    fun `활성 세션이 있으면 payload를 전송한다`() {
        val registry = NotificationSessionRegistry(::TestEmitter)
        val emitter = registry.connectRegister(2L) as TestEmitter

        val sent = registry.push(2L, TestPayload("TRANSFER_RECEIVED"))

        assertTrue(sent)
        assertEquals(1, emitter.sendCount)
        assertEquals("TRANSFER_RECEIVED", emitter.lastPayload.get()?.type)
    }

    @Test
    fun `활성 세션이 없으면 false를 반환한다`() {
        val registry = NotificationSessionRegistry(::TestEmitter)

        val sent = registry.push(999L, TestPayload("TRANSFER_RECEIVED"))

        assertFalse(sent)
    }

    @Test
    fun `연결이 완료되면 세션을 제거한다`() {
        val registry = NotificationSessionRegistry(::TestEmitter)
        val emitter = registry.connectRegister(3L)

        emitter.complete()

        assertTrue(registry.find(3L) == null)
    }

    private data class TestPayload(
        val type: String,
    )

    private class TestEmitter : SseEmitter() {
        val lastPayload = AtomicReference<TestPayload?>()
        var sendCount: Int = 0
            private set

        private var completionCallback: Runnable? = null
        private var errorCallback: Consumer<Throwable>? = null

        override fun onCompletion(callback: Runnable) {
            completionCallback = callback
            super.onCompletion(callback)
        }

        override fun onError(callback: Consumer<Throwable>) {
            errorCallback = callback
            super.onError(callback)
        }

        override fun complete() {
            super.complete()
            completionCallback?.run()
        }

        override fun completeWithError(ex: Throwable) {
            super.completeWithError(ex)
            errorCallback?.accept(ex)
        }

        override fun send(`object`: Any) {
            lastPayload.set(`object` as TestPayload)
            sendCount += 1
        }
    }
}
