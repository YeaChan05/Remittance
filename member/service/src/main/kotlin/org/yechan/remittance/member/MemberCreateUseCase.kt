package org.yechan.remittance.member

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.PasswordHashEncoder

interface MemberCreateUseCase {
    fun register(props: MemberProps): MemberModel
}

private val log = KotlinLogging.logger {}

class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordHashEncoder: PasswordHashEncoder,
) : MemberCreateUseCase {
    override fun register(props: MemberProps): MemberModel {
        log.info { "member.register.start" }
        memberRepository.findByEmail(props.email)?.let {
            log.warn { "member.register.duplicate_email email=${props.email}" }
            throw MemberException("Email already exists: ${props.email}")
        }

        log.info { "member.register.persist" }
        return memberRepository.save(EncodedMemberProps(props))
    }

    private inner class EncodedMemberProps(
        private val props: MemberProps,
    ) : MemberProps {
        override val name: String
            get() = props.name

        override val email: String
            get() = props.email

        override val password: String
            get() = try {
                log.debug { "member.register.password_hashing" }
                passwordHashEncoder.encode(props.password)
            } catch (ex: IllegalArgumentException) {
                log.warn { "member.register.invalid_password" }
                throw MemberException("Invalid password: ${props.password}, ${ex.message}")
            }
    }
}
