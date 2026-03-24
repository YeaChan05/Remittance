package org.yechan.remittance.member

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
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
