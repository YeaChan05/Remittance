package org.yechan.remittance.member

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(MemberBeanRegistrar::class)
@AutoConfiguration
class MemberAutoConfiguration
class MemberBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<MemberCreateUseCase> {
            MemberService(bean(), bean())
        }

        registerBean<MemberQueryUseCase> {
            MemberQueryService(bean(), bean(), bean())
        }

        registerBean<MemberAuthQueryUseCase> {
            MemberAuthQueryService(bean(), bean())
        }

        registerBean<MemberExistenceQueryUseCase> {
            MemberExistenceQueryService(bean())
        }
    })
