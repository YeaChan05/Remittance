package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.concurrent.atomic.AtomicBoolean

class TransferSnapshotBeanRegistrarTest {
    @Test
    fun `자동 설정은 애플리케이션 ObjectMapper를 우선 사용한다`() {
        val used = AtomicBoolean(false)
        val objectMapper = object : ObjectMapper() {
            override fun writeValueAsString(value: Any?): String {
                used.set(true)
                return super.writeValueAsString(value)
            }
        }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("objectMapper", objectMapper)
            register(TestConfiguration::class.java)
            refresh()
        }

        val transferSnapshotUtil = context.getBean(TransferSnapshotUtil::class.java)

        transferSnapshotUtil.toSnapshot(
            TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, 1L, null),
        )

        assertThat(used.get()).isTrue()

        context.close()
    }

    @Test
    fun `자동 설정은 ObjectMapper가 없어도 fallback으로 동작한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

        val transferSnapshotUtil = context.getBean(TransferSnapshotUtil::class.java)
        val snapshot = transferSnapshotUtil.toSnapshot(
            TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, 1L, null),
        )

        assertThat(snapshot).contains("\"status\":\"SUCCEEDED\"")

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferSnapshotBeanRegistrar::class)
    class TestConfiguration
}
