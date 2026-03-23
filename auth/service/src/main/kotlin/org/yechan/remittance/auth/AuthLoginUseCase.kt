package org.yechan.remittance.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.AuthTokenValue
import org.yechan.remittance.TokenGenerator

fun interface AuthLoginUseCase {
    fun login(props: AuthLoginProps): AuthTokenValue
}

private val log = KotlinLogging.logger {}

class AuthService(
    private val memberAuthClient: MemberAuthClient,
    private val tokenGenerator: TokenGenerator,
) : AuthLoginUseCase {
    override fun login(props: AuthLoginProps): AuthTokenValue {
        log.info { "auth.login.start" }
        val result = memberAuthClient.verify(props.email, props.password)
        if (!result.valid) {
            log.warn { "auth.login.invalid_credentials" }
            throw AuthInvalidCredentialException("Invalid credentials")
        }
        log.info { "auth.login.success memberId=${result.memberId}" }
        return tokenGenerator.generate(result.memberId)
    }
}
