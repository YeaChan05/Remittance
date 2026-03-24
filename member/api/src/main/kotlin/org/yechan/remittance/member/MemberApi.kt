package org.yechan.remittance.member

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.yechan.remittance.member.dto.MemberRegisterRequest
import org.yechan.remittance.member.dto.MemberRegisterResponse

@Tag(name = "Member", description = "Member API")
interface MemberApi {
    @Operation(summary = "Register member", description = "Registers a new member")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Registration succeeded",
            content = [Content()],
        ),
    )
    fun register(request: MemberRegisterRequest): ResponseEntity<MemberRegisterResponse>
}
