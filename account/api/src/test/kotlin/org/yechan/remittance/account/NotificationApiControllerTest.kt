package org.yechan.remittance.account

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class NotificationApiControllerTest {
    @Test
    fun `구독 연결 요청은 emitter를 등록한다`() {
        val registry = NotificationSessionRegistry(::TestEmitter)
        val controller = NotificationApiController(registry)

        val emitter = controller.connect(10L)

        assertNotNull(emitter)
        assertNotNull(registry.find(10L))
    }

    private class TestEmitter : SseEmitter()
}
