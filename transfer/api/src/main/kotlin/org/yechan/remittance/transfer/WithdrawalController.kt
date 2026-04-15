package org.yechan.remittance.transfer

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.transfer.dto.WithdrawalRequest

@RestController
@RequestMapping("/withdrawals", version = "v1")
class WithdrawalController(
    private val transferCreateUseCase: TransferCreateUseCase,
) {
    @PostMapping("/{idempotencyKey}")
    fun withdraw(
        @LoginUserId memberId: Long,
        @PathVariable idempotencyKey: String,
        @RequestBody @Valid request: WithdrawalRequest,
    ): TransferResult = transferCreateUseCase.transfer(memberId, idempotencyKey, request)
}
