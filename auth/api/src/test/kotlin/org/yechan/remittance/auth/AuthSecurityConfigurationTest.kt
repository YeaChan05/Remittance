package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthSecurityConfigurationTest {
    @Test
    fun `보안 설정은 로그인 요청 customizer를 노출한다`() {
        val configuration = AuthSecurityConfiguration()

        val customizer = configuration.authorizeHttpRequestsCustomizer()

        assertThat(customizer).isNotNull
    }
}
