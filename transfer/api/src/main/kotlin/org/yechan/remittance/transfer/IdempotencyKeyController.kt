package org.yechan.remittance.transfer

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse

@RestController
@RequestMapping("/idempotency-keys")
class IdempotencyKeyController(
    private val idempotencyKeyCreateUseCase: IdempotencyKeyCreateUseCase
) : IdempotencyKeyApi {
    @PostMapping
    override fun create(
        @LoginUserId memberId: Long,
        @RequestParam(required = false) scope: IdempotencyKeyProps.IdempotencyScopeValue?
    ): ResponseEntity<IdempotencyKeyCreateResponse> {
        val resolvedScope = scope ?: IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        val created = idempotencyKeyCreateUseCase.create(IdempotencyKeyCreateCommand(memberId, resolvedScope))
        return ResponseEntity.ok(
            IdempotencyKeyCreateResponse(created.idempotencyKey, requireNotNull(created.expiresAt))
        )
    }

    private data class IdempotencyKeyCreateCommand(
        override val memberId: Long,
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue
    ) : IdempotencyKeyCreateProps
}
