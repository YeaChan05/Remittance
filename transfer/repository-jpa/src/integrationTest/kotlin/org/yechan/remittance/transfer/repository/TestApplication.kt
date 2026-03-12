package org.yechan.remittance.transfer.repository

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

@Import(TransferRepositoryAutoConfiguration::class)
@EnableAutoConfiguration
@SpringBootConfiguration
class TestApplication
