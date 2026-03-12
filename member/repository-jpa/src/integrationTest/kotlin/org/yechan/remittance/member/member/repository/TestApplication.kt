package org.yechan.remittance.member.member.repository

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import
import org.yechan.remittance.member.repository.MemberRepositoryAutoConfiguration

@Import(MemberRepositoryAutoConfiguration::class)
@EnableAutoConfiguration
@SpringBootConfiguration
class TestApplication
