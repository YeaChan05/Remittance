package org.yechan.remittance.member

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.PasswordHashEncoder

fun interface MemberAuthenticationQueryUseCase {
    fun verify(props: MemberLoginProps): MemberAuthenticationResult
}

private val log = KotlinLogging.logger {}

class MemberAuthenticationQueryService(
    private val memberRepository: MemberRepository,
    private val passwordHashEncoder: PasswordHashEncoder,
) : MemberAuthenticationQueryUseCase {
    override fun verify(props: MemberLoginProps): MemberAuthenticationResult {
        log.info { "member.authentication.verify.start" }
        val member = memberRepository.findByEmail(props.email)
        if (member == null) {
            log.warn { "member.authentication.verify.not_found" }
            return MemberAuthenticationResult(valid = false, memberId = 0L)
        }

        if (!passwordHashEncoder.matches(props.password, member.password)) {
            log.warn { "member.authentication.verify.password_mismatch memberId=${member.memberId}" }
            return MemberAuthenticationResult(valid = false, memberId = 0L)
        }

        log.info { "member.authentication.verify.success memberId=${member.memberId}" }
        return MemberAuthenticationResult(valid = true, memberId = member.memberId ?: 0L)
    }
}
