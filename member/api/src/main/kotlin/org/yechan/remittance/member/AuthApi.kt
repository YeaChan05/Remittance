package org.yechan.remittance.member

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse

@Tag(name = "Auth", description = "Authentication API")
interface AuthApi {
    @Operation(summary = "Login", description = "Authenticates a member")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login succeeded", content = [Content()]),
    )
    fun login(request: MemberLoginRequest): ResponseEntity<MemberLoginResponse>
}
