package org.yechan.remittance.account.repository

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

@Import(AccountRepositoryAutoConfiguration::class)
@EnableAutoConfiguration
@SpringBootConfiguration
class TestApplication
