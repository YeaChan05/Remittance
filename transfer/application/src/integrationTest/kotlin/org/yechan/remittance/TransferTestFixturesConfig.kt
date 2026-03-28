package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.transfer.config.TransferApplicationAccountStore
import org.yechan.remittance.transfer.config.TransferApplicationMemberStore

@TestConfiguration
class TransferTestFixturesConfig {
    @Bean
    fun transferTestFixtures(
        restTestClient: RestTestClient,
        em: EntityManager,
        transactionTemplate: TransactionTemplate,
        tokenGenerator: TokenGenerator,
        tokenVerifier: TokenVerifier,
        accountStore: TransferApplicationAccountStore,
        memberStore: TransferApplicationMemberStore,
    ): TransferTestFixtures = TransferTestFixtures(
        accountStore,
        memberStore,
        em,
        transactionTemplate,
        tokenGenerator,
        tokenVerifier,
    )
}
