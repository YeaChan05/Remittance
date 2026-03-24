package org.yechan.remittance.transfer

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.yechan.remittance.transfer.dto.TransferQueryResponse
import org.yechan.remittance.transfer.dto.TransferRequest
import java.time.LocalDateTime

@Tag(name = "Transfer", description = "Transfer API")
interface TransferApi {
    @Operation(
        summary = "Create a transfer",
        description = "Requests a transfer with an idempotency key",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Succeeded", content = [Content()]),
    )
    fun transfer(
        @Parameter(hidden = true) memberId: Long,
        @Parameter(description = "Idempotency key") idempotencyKey: String,
        props: TransferRequest,
    ): TransferResult

    @Operation(summary = "Query transfers", description = "Queries transfer history for an account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Succeeded", content = [Content()]),
    )
    fun query(
        @Parameter(hidden = true) memberId: Long,
        @Parameter(description = "Account id") accountId: Long,
        @Parameter(description = "UTC start time") from: LocalDateTime?,
        @Parameter(description = "UTC end time") to: LocalDateTime?,
        @Parameter(description = "Maximum number of items") limit: Int?,
    ): TransferQueryResponse
}
