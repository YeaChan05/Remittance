package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer

class AuthSecurityConfigurationTest {
    @Test
    fun `보안 설정은 로그인 요청 customizer를 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(AuthSecurityConfiguration::class.java)
            refresh()
        }

        val customizer = context.getBean(
            AuthorizeHttpRequestsCustomizer::class.java
        )

        assertThat(customizer).isNotNull

        context.close()
    }
}
