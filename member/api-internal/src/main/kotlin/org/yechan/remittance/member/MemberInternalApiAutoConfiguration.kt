package org.yechan.remittance.member

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
class MemberInternalApiAutoConfiguration {
    @Bean
    fun memberInternalApi(memberAuthQueryUseCase: MemberAuthQueryUseCase): MemberInternalApi {
        return MemberInternalAdapter(memberAuthQueryUseCase)
    }
}
