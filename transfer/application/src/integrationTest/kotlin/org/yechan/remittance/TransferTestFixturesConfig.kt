package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.transfer.TransferAccountClient
import org.yechan.remittance.transfer.TransferAccountLockCommand
import org.yechan.remittance.transfer.TransferAccountSnapshot
import org.yechan.remittance.transfer.TransferBalanceChangeCommand
import org.yechan.remittance.transfer.TransferLockedAccounts
import org.yechan.remittance.transfer.config.TransferApplicationAccountStore
import org.yechan.remittance.transfer.config.TransferApplicationMemberStore
import org.yechan.remittance.transfer.config.TransferInternalApiStubEnvironment

@TestConfiguration
class TransferTestFixturesConfig {
    @Bean
    fun transferApplicationAccountStore(): TransferApplicationAccountStore = TransferInternalApiStubEnvironment.accountStore

    @Bean
    fun transferApplicationMemberStore(): TransferApplicationMemberStore = TransferInternalApiStubEnvironment.memberStore

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

    @Bean
    fun transferAccountClientCommitAwarePostProcessor(): BeanPostProcessor = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(
            bean: Any,
            beanName: String,
        ): Any {
            if (bean !is TransferAccountClient || bean is CommitAwareTransferAccountClient) {
                return bean
            }
            return CommitAwareTransferAccountClient(bean)
        }
    }
}

private class CommitAwareTransferAccountClient(
    private val delegate: TransferAccountClient,
) : TransferAccountClient {
    override fun get(
        memberId: Long,
        accountId: Long,
    ): TransferAccountSnapshot? = delegate.get(memberId, accountId)

    override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = delegate.lock(command)

    override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        delegate.applyBalanceChange(command)
                    }
                },
            )
            return
        }
        delegate.applyBalanceChange(command)
    }
}
