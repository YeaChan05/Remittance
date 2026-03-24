package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi

@AutoConfiguration
class TransferInfrastructureBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferAccountClient> {
            TransferAccountClientAdapter(bean<AccountInternalApi>())
        }

        registerBean<TransferMemberClient> {
            TransferMemberClientAdapter(bean<MemberExistenceInternalApi>())
        }
    })
