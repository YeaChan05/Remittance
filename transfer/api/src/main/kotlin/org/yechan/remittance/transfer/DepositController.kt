package org.yechan.remittance.transfer

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.transfer.dto.DepositRequest

@RestController
@RequestMapping("/deposits")
class DepositController(
    private val transferCreateUseCase: TransferCreateUseCase,
) {
    @PostMapping("/{idempotencyKey}")
    fun deposit(
        @LoginUserId memberId: Long,
        @PathVariable idempotencyKey: String,
        @RequestBody @Valid request: DepositRequest,
    ): TransferResult = transferCreateUseCase.transfer(memberId, idempotencyKey, request)
}
