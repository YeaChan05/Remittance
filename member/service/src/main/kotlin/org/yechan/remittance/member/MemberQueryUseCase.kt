package org.yechan.remittance.member

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.PasswordHashEncoder
import org.yechan.remittance.Status.AUTHENTICATION_FAILED
import org.yechan.remittance.TokenGenerator

interface MemberQueryUseCase {
    fun login(props: MemberLoginProps): MemberTokenValue
}

class MemberQueryService(
    private val memberRepository: MemberRepository,
    private val passwordHashEncoder: PasswordHashEncoder,
    private val tokenGenerator: TokenGenerator
) : MemberQueryUseCase {
    private val log = KotlinLogging.logger {}

    override fun login(props: MemberLoginProps): MemberTokenValue {
        log.info { "member.login.start" }
        val member =
            memberRepository.findByEmail(props.email)
                ?: run {
                    log.warn { "member.login.not_found" }
                    throw MemberException("Member not found")
                }

        if (!passwordHashEncoder.matches(props.password, member.password)) {
            log.warn { "member.login.invalid_credentials memberId=${member.memberId}" }
            throw MemberPermissionDeniedException(AUTHENTICATION_FAILED, "Invalid credentials")
        }

        val token = tokenGenerator.generate(member.memberId)
        log.info { "member.login.success memberId=${member.memberId}" }
        return MemberTokenValue(token.accessToken, token.refreshToken, token.expiresIn)
    }
}
