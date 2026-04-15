package org.yechan.remittance

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@AutoConfiguration
class RestTestClientTestConfiguration {
    @Bean
    fun restTestClient(context: WebApplicationContext): RestTestClient {
        val mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
        return RestTestClient.bindTo(mockMvc)
            .defaultHeader("X-API-Version", "v1")
            .build()
    }
}
