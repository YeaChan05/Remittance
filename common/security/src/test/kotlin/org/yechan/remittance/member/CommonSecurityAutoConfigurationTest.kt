package org.yechan.remittance.member

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.TokenGenerator

@SpringBootTest(
    classes = [
        CommonSecurityAutoConfigurationTest.TestApplication::class,
        CommonSecurityAutoConfigurationTest.RestTestClientConfiguration::class,
    ],
)
@TestPropertySource(
    properties = [
        "auth.token.salt=test-salt",
        "auth.token.access-expires-in=3600",
        "auth.token.refresh-expires-in=7200",
    ],
)
class CommonSecurityAutoConfigurationTest {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenGenerator: TokenGenerator

    @Test
    fun `requests without token are unauthorized`() {
        restTestClient.get()
            .uri("/secure")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `requests with invalid token are unauthorized`() {
        restTestClient.get()
            .uri("/secure")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `requests with valid token are allowed`() {
        val token = tokenGenerator.generate(1L).accessToken

        restTestClient.get()
            .uri("/secure")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("1")
    }

    @Test
    fun `additional customizers are applied before default closing rule`() {
        restTestClient.get()
            .uri("/open")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("open")

        restTestClient.get()
            .uri("/secure")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    class TestApplication {
        @RestController
        class TestController {
            @GetMapping("/open")
            fun open(): String = "open"

            @GetMapping("/secure")
            fun secure(authentication: Authentication): String = authentication.name
        }
    }

    @TestConfiguration
    class RestTestClientConfiguration {
        @Bean
        fun restTestClient(context: WebApplicationContext): RestTestClient {
            val mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                    .apply<DefaultMockMvcBuilder>(springSecurity())
                    .build()
            return RestTestClient.bindTo(mockMvc).build()
        }

        @Bean
        fun openEndpointCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            0,
        ) { registry -> registry.requestMatchers("/open").permitAll() }
    }
}
