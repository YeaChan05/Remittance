package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TransferSnapshotConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    @Bean
    fun transferSnapshotUtil(objectMapper: ObjectMapper): TransferSnapshotUtil {
        return TransferSnapshotUtil(objectMapper)
    }
}
