package org.yechan.remittance

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.yechan.remittance.transfer.TransferAccountClient
import org.yechan.remittance.transfer.TransferAccountIdentifier
import org.yechan.remittance.transfer.TransferAccountLockCommand
import org.yechan.remittance.transfer.TransferAccountSnapshot
import org.yechan.remittance.transfer.TransferBalanceChangeCommand
import org.yechan.remittance.transfer.TransferIdentifier
import org.yechan.remittance.transfer.TransferLockedAccounts
import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferQueryCondition
import org.yechan.remittance.transfer.TransferRepository
import org.yechan.remittance.transfer.TransferRequestProps
import org.yechan.remittance.transfer.config.TransferApplicationAccountStore
import org.yechan.remittance.transfer.config.TransferApplicationMemberStore
import org.yechan.remittance.transfer.config.TransferInternalApiStubEnvironment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

@TestConfiguration
class TransferTestFixturesConfig {
    @Bean
    fun transferApplicationAccountStore(): TransferApplicationAccountStore = TransferInternalApiStubEnvironment.accountStore

    @Bean
    fun transferApplicationMemberStore(): TransferApplicationMemberStore = TransferInternalApiStubEnvironment.memberStore

    @Bean
    fun transferTestFixtures(
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

    @Bean
    fun transferFailureSwitch(): TransferFailureSwitch = TransferFailureSwitch()

    @Bean
    @Primary
    fun failureTransferRepository(
        delegate: TransferRepository,
        transferFailureSwitch: TransferFailureSwitch,
    ): TransferRepository = FailureTransferRepository(delegate, transferFailureSwitch)
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

class TransferFailureSwitch {
    private val enabled = AtomicBoolean(false)

    fun enable() {
        enabled.set(true)
    }

    fun disable() {
        enabled.set(false)
    }

    fun shouldFail(): Boolean = enabled.get()
}

private class FailureTransferRepository(
    private val delegate: TransferRepository,
    private val failureSwitch: TransferFailureSwitch,
) : TransferRepository {
    override fun save(props: TransferRequestProps): TransferModel {
        if (failureSwitch.shouldFail()) {
            throw IllegalStateException("Transfer save failed")
        }
        return delegate.save(props)
    }

    override fun findById(identifier: TransferIdentifier): TransferModel? = delegate.findById(identifier)

    override fun findCompletedByAccountId(
        identifier: TransferAccountIdentifier,
        condition: TransferQueryCondition,
    ): List<TransferModel> = delegate.findCompletedByAccountId(identifier, condition)

    override fun sumAmountByFromAccountIdAndScopeBetween(
        identifier: TransferAccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        from: LocalDateTime,
        to: LocalDateTime,
    ): BigDecimal = delegate.sumAmountByFromAccountIdAndScopeBetween(identifier, scope, from, to)
}
