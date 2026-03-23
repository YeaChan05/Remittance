package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(TransferSnapshotBeanRegistrar::class)
@Configuration
class TransferSnapshotConfiguration

class TransferSnapshotBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferSnapshotUtil> {
            TransferSnapshotUtil(beanProvider<ObjectMapper>().ifAvailable ?: ObjectMapper())
        }
    })
