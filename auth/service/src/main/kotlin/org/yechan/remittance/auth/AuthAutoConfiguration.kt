package org.yechan.remittance.auth

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.yechan.remittance.TokenGenerator

@AutoConfiguration
class AuthAutoConfiguration {
    @Bean
    fun authLoginUseCase(
        memberAuthClient: MemberAuthClient,
        tokenGenerator: TokenGenerator
    ): AuthLoginUseCase {
        return AuthService(memberAuthClient, tokenGenerator)
    }
}
