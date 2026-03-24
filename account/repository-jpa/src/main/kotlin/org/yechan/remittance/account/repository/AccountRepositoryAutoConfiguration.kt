package org.yechan.remittance.account.repository

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.account.ProcessedEventRepository

@EntityScan(basePackageClasses = [AccountEntity::class, ProcessedEventEntity::class])
@EnableJpaRepositories(basePackageClasses = [AccountJpaRepository::class, ProcessedEventJpaRepository::class])
class AccountRepositoryAutoConfiguration

@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
class AccountRepositoryBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AccountRepository> {
            AccountRepositoryImpl(bean())
        }

        registerBean<ProcessedEventRepository> {
            ProcessedEventRepositoryImpl(bean())
        }
    })
