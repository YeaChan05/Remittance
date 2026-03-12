package org.yechan.remittance.member

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.PasswordHashEncoder

fun interface MemberAuthQueryUseCase {
    fun verify(props: MemberLoginProps): MemberAuthValue
}

private val log = KotlinLogging.logger {}

class MemberAuthQueryService(
    private val memberRepository: MemberRepository,
    private val passwordHashEncoder: PasswordHashEncoder
) : MemberAuthQueryUseCase {
    override fun verify(props: MemberLoginProps): MemberAuthValue {
        log.info { "member.auth.verify.start" }
        val member = memberRepository.findByEmail(props.email)
        if (member == null) {
            log.warn { "member.auth.verify.not_found" }
            return MemberAuthValue(valid = false, memberId = 0L)
        }

        if (!passwordHashEncoder.matches(props.password, member.password)) {
            log.warn { "member.auth.verify.password_mismatch memberId=${member.memberId}" }
            return MemberAuthValue(valid = false, memberId = 0L)
        }

        log.info { "member.auth.verify.success memberId=${member.memberId}" }
        return MemberAuthValue(valid = true, memberId = member.memberId ?: 0L)
    }
}
