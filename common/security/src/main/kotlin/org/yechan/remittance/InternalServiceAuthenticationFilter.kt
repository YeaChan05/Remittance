package org.yechan.remittance

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

class InternalServiceAuthenticationFilter(
    private val properties: InternalServiceAuthProperties,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/internal/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader(INTERNAL_TOKEN_HEADER)
        val internalUserId = request.getHeader(INTERNAL_USER_ID_HEADER)

        try {
            if (token != properties.token) {
                throw BadCredentialsException("Invalid internal token")
            }

            val principal = internalUserId?.takeIf { it.isNotBlank() }?.let {
                if (it.toLongOrNull() == null) {
                    throw BadCredentialsException("Invalid internal user id")
                }
                it
            } ?: run {
                log.warn { "internal.auth.user_id_missing uri=${request.requestURI}" }
                INTERNAL_PRINCIPAL
            }

            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = UsernamePasswordAuthenticationToken(
                principal,
                token,
                AuthorityUtils.NO_AUTHORITIES,
            )
            SecurityContextHolder.setContext(context)
            filterChain.doFilter(request, response)
        } catch (ex: AuthenticationException) {
            authenticationEntryPoint.commence(request, response, ex)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    companion object {
        const val INTERNAL_TOKEN_HEADER = InternalRequestHeaders.TOKEN
        const val INTERNAL_USER_ID_HEADER = InternalRequestHeaders.USER_ID
        private const val INTERNAL_PRINCIPAL = "internal-service"
    }
}
