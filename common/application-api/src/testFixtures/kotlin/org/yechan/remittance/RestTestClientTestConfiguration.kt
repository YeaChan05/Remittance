package org.yechan.remittance

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.web.context.WebApplicationContext

@AutoConfiguration
class RestTestClientTestConfiguration {
    @Bean
    fun restTestClient(context: WebApplicationContext): RestTestClient {
        return RestTestClient.bindToApplicationContext(context).build()
    }
}
