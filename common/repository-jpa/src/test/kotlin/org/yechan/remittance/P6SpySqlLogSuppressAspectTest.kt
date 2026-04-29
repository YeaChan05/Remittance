package org.yechan.remittance

import org.aspectj.lang.ProceedingJoinPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.MDC

class P6SpySqlLogSuppressAspectTest {

    private val aspect = P6SpySqlLogSuppressAspect()

    @Test
    fun `join point 실행 동안 MDC suppress flag를 켠다`() {
        val joinPoint = mock(ProceedingJoinPoint::class.java)
        `when`(joinPoint.proceed()).thenAnswer {
            assertThat(P6SpySqlLogSuppressor.isSuppressed()).isTrue()
            assertThat(MDC.get(P6SpySqlLogSuppressor.MDC_KEY)).isEqualTo("true")
            "done"
        }

        val result = aspect.suppress(joinPoint)

        assertThat(result).isEqualTo("done")
        assertThat(P6SpySqlLogSuppressor.isSuppressed()).isFalse()
        assertThat(MDC.get(P6SpySqlLogSuppressor.MDC_KEY)).isNull()
    }

    @Test
    fun `기존 MDC 값은 suppress 구간 이후 복원한다`() {
        MDC.put(P6SpySqlLogSuppressor.MDC_KEY, "previous")
        try {
            P6SpySqlLogSuppressor.suppress {
                assertThat(MDC.get(P6SpySqlLogSuppressor.MDC_KEY)).isEqualTo("true")
            }

            assertThat(MDC.get(P6SpySqlLogSuppressor.MDC_KEY)).isEqualTo("previous")
        } finally {
            MDC.remove(P6SpySqlLogSuppressor.MDC_KEY)
        }
    }
}
