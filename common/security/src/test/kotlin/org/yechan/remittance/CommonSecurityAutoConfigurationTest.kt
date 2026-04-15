package org.yechan.remittance

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.ApiVersionInserter
import org.springframework.web.context.WebApplicationContext

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
        "auth.internal.token=test-internal-token",
    ],
)
class CommonSecurityAutoConfigurationTest {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var tokenGenerator: TokenGenerator

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `requests without token are unauthorized`() {
        restTestClient.get()
            .uri("/secure")
            .header("X-API-Version", "v1")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `requests with invalid token are unauthorized`() {
        restTestClient.get()
            .uri("/secure")
            .header("X-API-Version", "v1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `requests with valid token are allowed`() {
        val token = tokenGenerator.generate(1L).accessToken

        restTestClient.get()
            .uri("/secure")
            .header("X-API-Version", "v1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("1")
    }

    @Test
    fun `internal requests with valid internal token are allowed`() {
        restTestClient.get()
            .uri("/internal/secure")
            .header("X-API-Version", "v1")
            .header(
                InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER,
                "test-internal-token",
            )
            .header(InternalServiceAuthenticationFilter.INTERNAL_USER_ID_HEADER, "7")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("7")
    }

    @Test
    fun `internal requests without user id keep temporary compatibility`() {
        restTestClient.get()
            .uri("/internal/secure")
            .header("X-API-Version", "v1")
            .header(
                InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER,
                "test-internal-token",
            )
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("internal-service")
    }

    @Test
    fun `internal requests without internal token are unauthorized`() {
        restTestClient.get()
            .uri("/internal/secure")
            .header("X-API-Version", "v1")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `external requests do not trust internal user id header`() {
        restTestClient.get()
            .uri("/secure")
            .header("X-API-Version", "v1")
            .header(InternalServiceAuthenticationFilter.INTERNAL_USER_ID_HEADER, "7")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `additional customizers are applied before default closing rule`() {
        restTestClient.get()
            .uri("/open")
            .header("X-API-Version", "v1")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo("open")

        restTestClient.get()
            .uri("/secure")
            .header("X-API-Version", "v1")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    class TestApplication {
        @RestController
        @RequestMapping(version = "v1")
        class TestController {
            @GetMapping("/open")
            fun open(): String = "open"

            @GetMapping("/secure")
            fun secure(authentication: Authentication): String = authentication.name

            @GetMapping("/internal/secure")
            fun internalSecure(authentication: Authentication): String = authentication.name
        }
    }

    @TestConfiguration
    class RestTestClientConfiguration {
        @Bean
        fun restTestClient(context: WebApplicationContext): RestTestClient {
            val mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                    .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                    .build()
            return RestTestClient.bindTo(mockMvc)
                .apiVersionInserter(ApiVersionInserter.useHeader("X-API-Version"))
                .build()
        }

        @Bean
        fun openEndpointCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            0,
        ) { registry -> registry.requestMatchers("/open").permitAll() }
    }
}
