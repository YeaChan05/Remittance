package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi

@Import(TransferInfrastructureBeanRegistrar::class)
@AutoConfiguration
class TransferInfrastructureAutoConfiguration

class TransferInfrastructureBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferAccountClient> {
            TransferAccountClientAdapter(bean<AccountInternalApi>())
        }

        registerBean<TransferMemberClient> {
            TransferMemberClientAdapter(bean<MemberExistenceInternalApi>())
        }
    })
