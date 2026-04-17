package org.yechan.remittance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.accept.HeaderApiVersionResolver

class SwaggerOpenApiApiVersionResolverTest {
    private val resolver = SwaggerOpenApiApiVersionResolver(
        HeaderApiVersionResolver("X-API-Version"),
    )

    @Test
    fun `swagger openapi paths fall back to current api version when header is missing`() {
        val exemptPaths =
            listOf(
                "/swagger-ui.html",
                "/swagger-ui/index.html",
                "/v3/api-docs",
                "/v3/api-docs/grouped",
                "/v3/api-docs.yaml",
                "/swagger-resources/configuration/ui",
                "/webjars/swagger-ui.css",
                "/swagger/custom",
            )

        exemptPaths.forEach { path ->
            val request = MockHttpServletRequest("GET", path)

            assertThat(resolver.resolveVersion(request)).isEqualTo("v1")
        }
    }

    @Test
    fun `non swagger paths keep delegated version resolution`() {
        val missingVersionRequest = MockHttpServletRequest("GET", "/members")
        val explicitVersionRequest = MockHttpServletRequest("GET", "/members").apply {
            addHeader("X-API-Version", "v2")
        }

        assertThat(resolver.resolveVersion(missingVersionRequest)).isNull()
        assertThat(resolver.resolveVersion(explicitVersionRequest)).isEqualTo("v2")
    }
}
