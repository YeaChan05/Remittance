package org.yechan.remittance.member

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.yechan.remittance.PasswordHashEncoder
import org.yechan.remittance.TokenGenerator

@AutoConfiguration
class MemberAutoConfiguration {
    @Bean
    fun memberCreateUseCase(
        memberRepository: MemberRepository,
        passwordHashEncoder: PasswordHashEncoder
    ): MemberCreateUseCase {
        return MemberService(memberRepository, passwordHashEncoder)
    }

    @Bean
    fun memberQueryUseCase(
        memberRepository: MemberRepository,
        passwordHashEncoder: PasswordHashEncoder,
        tokenGenerator: TokenGenerator
    ): MemberQueryUseCase {
        return MemberQueryService(memberRepository, passwordHashEncoder, tokenGenerator)
    }

    @Bean
    fun memberAuthQueryUseCase(
        memberRepository: MemberRepository,
        passwordHashEncoder: PasswordHashEncoder
    ): MemberAuthQueryUseCase {
        return MemberAuthQueryService(memberRepository, passwordHashEncoder)
    }
}
