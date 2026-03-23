package org.yechan.remittance.account

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.account.dto.AccountCreateRequest
import org.yechan.remittance.account.dto.AccountCreateResponse
import org.yechan.remittance.account.dto.AccountDeleteResponse
import java.math.BigDecimal

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountCreateUseCase: AccountCreateUseCase,
    private val accountDeleteUseCase: AccountDeleteUseCase,
) : AccountApi {
    @PostMapping
    override fun create(
        @LoginUserId memberId: Long,
        @RequestBody @Valid request: AccountCreateRequest,
    ): ResponseEntity<AccountCreateResponse> {
        val account =
            accountCreateUseCase.create(
                AccountCreateCommand(
                    memberId,
                    request.bankCode,
                    request.accountNumber,
                    request.accountName,
                ),
            )
        return ResponseEntity.ok(AccountCreateResponse(account.accountId, account.accountName))
    }

    @DeleteMapping("/{accountId}")
    override fun delete(
        @LoginUserId memberId: Long,
        @PathVariable accountId: Long,
    ): ResponseEntity<AccountDeleteResponse> {
        val account = accountDeleteUseCase.delete(AccountDeleteCommand(memberId, accountId))
        return ResponseEntity.ok(AccountDeleteResponse(requireNotNull(account.accountId)))
    }

    private data class AccountCreateCommand(
        override val memberId: Long?,
        override val bankCode: String,
        override val accountNumber: String,
        override val accountName: String,
    ) : AccountProps {
        override val balance: BigDecimal = BigDecimal.ZERO
    }

    private data class AccountDeleteCommand(
        override val memberId: Long,
        override val accountId: Long,
    ) : AccountDeleteProps
}
