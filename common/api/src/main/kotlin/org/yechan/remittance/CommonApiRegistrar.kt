package org.yechan.remittance

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.web.accept.ApiVersionResolver
import org.springframework.web.accept.HeaderApiVersionResolver
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Import(GlobalExceptionHandler::class)
class CommonApiRegistrar

@AutoConfiguration
class CommonApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<LoginUserIdArgumentResolver> {
            LoginUserIdArgumentResolver()
        }

        registerBean<WebMvcConfigurer> {
            val loginUserIdArgumentResolver = bean<LoginUserIdArgumentResolver>()

            object : WebMvcConfigurer {
                override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                    resolvers.add(loginUserIdArgumentResolver)
                }

                override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
                    configurer.useVersionResolver(
                        SwaggerOpenApiApiVersionResolver(
                            HeaderApiVersionResolver(API_VERSION_HEADER),
                        ),
                    )
                }
            }
        }
    })

private const val API_VERSION_HEADER = "X-API-Version"

/**
 * API versioning is globally enabled, but Springdoc endpoints themselves are not versioned.
 * Return the current public version for those documentation routes so browser requests
 * without the header can reach Swagger/OpenAPI handlers.
 */
class SwaggerOpenApiApiVersionResolver(
    private val delegate: ApiVersionResolver,
    private val swaggerFallbackVersion: String = "v1",
) : ApiVersionResolver {
    override fun resolveVersion(request: HttpServletRequest): String? {
        val resolvedVersion = delegate.resolveVersion(request)
        if (resolvedVersion != null) {
            return resolvedVersion
        }

        val requestPath = request.requestURI.removePrefix(request.contextPath)
        return if (requestPath in exactSwaggerPaths || swaggerPrefixes.any(requestPath::startsWith)) {
            swaggerFallbackVersion
        } else {
            null
        }
    }

    companion object {
        private val exactSwaggerPaths =
            setOf(
                "/swagger-ui.html",
                "/v3/api-docs",
                "/v3/api-docs.yaml",
            )

        private val swaggerPrefixes =
            listOf(
                "/swagger-ui/",
                "/v3/api-docs/",
                "/swagger-resources/",
                "/webjars/",
                "/swagger/",
            )
    }
}
