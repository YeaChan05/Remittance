package org.yechan.remittance.member.repository

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.yechan.remittance.member.MemberRepository

@AutoConfiguration(before = [DataJpaRepositoriesAutoConfiguration::class])
@EntityScan(basePackageClasses = [MemberEntity::class])
@EnableJpaRepositories(basePackageClasses = [MemberJpaRepository::class])
class MemberRepositoryAutoConfiguration {
    @Bean
    fun memberRepository(repository: MemberJpaRepository): MemberRepository {
        return MemberRepositoryImpl(repository)
    }
}
