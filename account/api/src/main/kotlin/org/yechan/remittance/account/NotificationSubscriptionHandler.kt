package org.yechan.remittance.account

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

fun interface NotificationSubscriptionHandler {
    fun subscribe(memberId: Long): SseEmitter
}

class NotificationSessionRegistry(
    private val emitterSupplier: () -> SseEmitter = { SseEmitter() },
) : NotificationSubscriptionHandler {
    private val sessions = ConcurrentHashMap<Long, SseEmitter>()

    override fun subscribe(memberId: Long): SseEmitter {
        val emitter = emitterSupplier()
        sessions[memberId] = emitter
        emitter.onCompletion { sessions.remove(memberId) }
        emitter.onTimeout { sessions.remove(memberId) }
        emitter.onError { sessions.remove(memberId) }
        return emitter
    }

    fun find(memberId: Long): SseEmitter? = sessions[memberId]

    fun push(
        memberId: Long,
        payload: Any,
    ): Boolean {
        val emitter = sessions[memberId] ?: return false
        return try {
            emitter.send(payload)
            true
        } catch (_: IOException) {
            sessions.remove(memberId)
            false
        } catch (_: IllegalStateException) {
            sessions.remove(memberId)
            false
        }
    }
}
