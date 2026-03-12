package org.yechan.remittance.auth

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.yechan.remittance.member.MemberInternalApi

@AutoConfiguration
class AuthClientConfiguration {
    @Bean
    fun memberAuthClient(memberInternalApi: MemberInternalApi): MemberAuthClient {
        return MemberAuthClientAdapter(memberInternalApi)
    }
}
