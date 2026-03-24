package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
class TransferSnapshotBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferSnapshotUtil> {
            TransferSnapshotUtil(beanProvider<ObjectMapper>().ifAvailable ?: ObjectMapper())
        }
    })
