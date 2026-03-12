package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
class TransferTestFixturesConfig {
    @Bean
    fun transferTestFixtures(
        restTestClient: RestTestClient,
        em: EntityManager,
        transactionTemplate: TransactionTemplate,
        tokenVerifier: TokenVerifier
    ): TransferTestFixtures {
        return TransferTestFixtures(restTestClient, em, transactionTemplate, tokenVerifier)
    }
}
