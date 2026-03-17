package org.yechan.remittance.account.repository

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.account.ProcessedEventRepository

@Import(AccountRepositoryBeanRegistrar::class)
@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
@EntityScan(basePackageClasses = [AccountEntity::class, ProcessedEventEntity::class])
@EnableJpaRepositories(basePackageClasses = [AccountJpaRepository::class, ProcessedEventJpaRepository::class])
class AccountRepositoryAutoConfiguration

class AccountRepositoryBeanRegistrar : BeanRegistrarDsl({
    registerBean<AccountRepository> {
        AccountRepositoryImpl(bean())
    }

    registerBean<ProcessedEventRepository> {
        ProcessedEventRepositoryImpl(bean())
    }
})
