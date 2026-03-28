package org.yechan.remittance.member.internal.adapter

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.yechan.remittance.member.MemberAuthenticationQueryUseCase
import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberAuthenticationInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi

@AutoConfiguration
class MemberInternalApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<MemberAuthenticationInternalApi> {
            MemberAuthenticationInternalAdapter(bean<MemberAuthenticationQueryUseCase>())
        }

        registerBean<MemberExistenceInternalApi> {
            MemberExistenceInternalAdapter(bean<MemberExistenceQueryUseCase>())
        }
    })
