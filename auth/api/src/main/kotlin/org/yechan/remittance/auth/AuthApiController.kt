package org.yechan.remittance.auth

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthApiController(
    private val authLoginUseCase: AuthLoginUseCase
) {
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: AuthLoginRequest
    ): ResponseEntity<AuthLoginResponse> {
        val token = authLoginUseCase.login(request)
        val response = AuthLoginResponse(token.accessToken, token.refreshToken, token.expiresIn)
        return ResponseEntity.ok(response)
    }
}
