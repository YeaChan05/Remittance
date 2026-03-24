package org.yechan.remittance.member.repository

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.member.MemberRepository

@EntityScan(basePackageClasses = [MemberEntity::class])
@EnableJpaRepositories(basePackageClasses = [MemberJpaRepository::class])
class MemberRepositoryAutoConfiguration

@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
class MemberRepositoryBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<MemberRepository> {
            MemberRepositoryImpl(bean())
        }
    })
