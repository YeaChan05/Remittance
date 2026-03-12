package org.yechan.remittance

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(
    classes = [AggregateApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class IntegrationTest : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Test
    fun `actuator endpoint가 응답한다`() {
        restTestClient.get().uri("/actuator/health")
            .exchange()
            .expectStatus().isOk

        restTestClient.get().uri("/actuator/info")
            .exchange()
            .expectStatus().isOk
    }
}
