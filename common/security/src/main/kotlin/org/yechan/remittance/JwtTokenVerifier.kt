package org.yechan.remittance

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.util.Collections
import javax.crypto.SecretKey
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication

class JwtTokenVerifier(
    salt: String
) : TokenVerifier {
    private val secretKey: SecretKey

    init {
        val secret = SECRET + salt
        secretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    override fun verify(token: String): Authentication {
        try {
            val claims =
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            val subject = claims.subject ?: throw BadCredentialsException("Invalid token subject")
            return UsernamePasswordAuthenticationToken(subject, token, Collections.emptyList())
        } catch (e: JwtException) {
            throw BadCredentialsException("Invalid token", e)
        } catch (e: IllegalArgumentException) {
            throw BadCredentialsException("Invalid token", e)
        }
    }

    private companion object {
        const val SECRET = "member-token-secret-member-token-secret"
    }
}
