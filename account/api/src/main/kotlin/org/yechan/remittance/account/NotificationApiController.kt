package org.yechan.remittance.account

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.yechan.remittance.LoginUserId

@RestController
@RequestMapping("/notification", version = "v1")
class NotificationApiController(
    private val handler: NotificationSubscriptionHandler,
) : NotificationApi {
    @GetMapping("/subscribe")
    override fun connect(@LoginUserId memberId: Long): SseEmitter = handler.subscribe(memberId)
}
