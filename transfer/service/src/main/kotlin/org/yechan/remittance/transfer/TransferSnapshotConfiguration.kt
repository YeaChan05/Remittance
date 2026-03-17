package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(
    TransferSnapshotBeanRegistrar::class,
    TransferSnapshotObjectMapperConfiguration::class
)
@Configuration
class TransferSnapshotConfiguration

class TransferSnapshotBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferSnapshotUtil> {
        TransferSnapshotUtil(bean())
    }
})

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ObjectMapper::class)
@Import(TransferSnapshotObjectMapperBeanRegistrar::class)
class TransferSnapshotObjectMapperConfiguration

class TransferSnapshotObjectMapperBeanRegistrar : BeanRegistrarDsl({
    registerBean<ObjectMapper> {
        ObjectMapper()
    }
})
