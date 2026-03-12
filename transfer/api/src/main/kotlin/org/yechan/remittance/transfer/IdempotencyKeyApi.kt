package org.yechan.remittance.transfer

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse

@Tag(name = "IdempotencyKey", description = "Idempotency key API")
interface IdempotencyKeyApi {
    @Operation(summary = "Create an idempotency key", description = "Creates an idempotency key for a request scope")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Created", content = [Content()])
    )
    fun create(
        @Parameter(hidden = true) memberId: Long,
        @Parameter(description = "Scope (TRANSFER/DEPOSIT/WITHDRAW)")
        scope: IdempotencyKeyProps.IdempotencyScopeValue?
    ): ResponseEntity<IdempotencyKeyCreateResponse>
}
