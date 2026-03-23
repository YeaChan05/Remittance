package org.yechan.remittance.member

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.member.dto.MemberLoginRequest
import org.yechan.remittance.member.dto.MemberLoginResponse

@RestController
class AuthController(
    private val memberQueryUseCase: MemberQueryUseCase,
) : AuthApi {
    @PostMapping("/login")
    override fun login(
        @RequestBody @Valid request: MemberLoginRequest,
    ): ResponseEntity<MemberLoginResponse> {
        val token = memberQueryUseCase.login(request)
        val response = MemberLoginResponse.from(token)
        return ResponseEntity.ok(response)
    }
}

fun MemberLoginResponse.Companion.from(token: MemberTokenValue): MemberLoginResponse = MemberLoginResponse(
    accessToken = token.accessToken,
    refreshToken = token.refreshToken,
    expiresIn = token.expiresIn,
)
