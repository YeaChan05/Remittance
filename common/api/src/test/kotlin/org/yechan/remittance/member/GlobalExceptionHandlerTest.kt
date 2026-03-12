package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.yechan.remittance.BusinessException
import org.yechan.remittance.GlobalExceptionHandler

class GlobalExceptionHandlerTest {
    @Test
    fun `business exception handling returns internal server error and message body`() {
        val handler = GlobalExceptionHandler()
        val exception = BusinessException("message")

        val response = handler.handleBusinessException(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isSameAs(exception.message)
    }

    @Test
    fun `business exception handling accepts subclass exceptions`() {
        val handler = GlobalExceptionHandler()
        val exception = SomeBusinessException("message")

        val response = handler.handleBusinessException(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body).isSameAs(exception.message)
    }

    private class SomeBusinessException(
        message: String
    ) : BusinessException(message)
}
