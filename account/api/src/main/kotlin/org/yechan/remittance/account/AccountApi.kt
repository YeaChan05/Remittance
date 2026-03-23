package org.yechan.remittance.account

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.yechan.remittance.account.dto.AccountCreateRequest
import org.yechan.remittance.account.dto.AccountCreateResponse
import org.yechan.remittance.account.dto.AccountDeleteResponse

@Tag(name = "Account", description = "Account API")
interface AccountApi {
    @Operation(summary = "Create account", description = "Registers a member account.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success", content = [Content()]),
    )
    fun create(
        @Parameter(hidden = true) memberId: Long,
        request: AccountCreateRequest,
    ): ResponseEntity<AccountCreateResponse>

    @Operation(summary = "Delete account", description = "Deletes a member account.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success", content = [Content()]),
    )
    fun delete(
        @Parameter(hidden = true) memberId: Long,
        @Parameter(description = "Account ID") accountId: Long,
    ): ResponseEntity<AccountDeleteResponse>
}
