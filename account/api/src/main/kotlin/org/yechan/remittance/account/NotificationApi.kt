package org.yechan.remittance.account

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface NotificationApi {
    fun connect(memberId: Long): SseEmitter
}
