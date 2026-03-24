package org.yechan.remittance.member.internal.adapter

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.yechan.remittance.member.MemberAuthQueryUseCase
import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi
import org.yechan.remittance.member.internal.contract.MemberInternalApi

@AutoConfiguration
class MemberInternalApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<MemberInternalApi> {
            MemberInternalAdapter(bean<MemberAuthQueryUseCase>())
        }

        registerBean<MemberExistenceInternalApi> {
            MemberExistenceInternalAdapter(bean<MemberExistenceQueryUseCase>())
        }
    })
