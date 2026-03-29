package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
class AggregateTransferTestFixturesConfig {
    @Bean
    fun aggregateTransferTestFixtures(
        restTestClient: RestTestClient,
        entityManager: EntityManager,
        transactionTemplate: TransactionTemplate,
        tokenVerifier: TokenVerifier,
    ): AggregateTransferTestFixtures = AggregateTransferTestFixtures(
        restTestClient,
        entityManager,
        transactionTemplate,
        tokenVerifier,
    )
}
