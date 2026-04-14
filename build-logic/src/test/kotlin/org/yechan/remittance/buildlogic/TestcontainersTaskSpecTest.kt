package org.yechan.remittance.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestcontainersTaskSpecTest {
    @Test
    fun `isolate는 provider 이름을 소문자로 정규화한다`() {
        val spec = TestcontainersTaskSpec("integrationTest")

        spec.isolate("MySQL")

        assertTrue("mysql" in spec.isolatedContainerKeys)
    }

    @Test
    fun `non aggregate mysql은 공용 share scope를 사용한다`() {
        val spec = TestcontainersTaskSpec("integrationTest")
        spec.use("mysql")

        assertEquals(
            NON_AGGREGATE_MYSQL_SHARE_SCOPE_KEY,
            resolveProviderShareScopeKey(
                projectPath = ":member:application",
                taskName = "integrationTest",
                executionStackKey = "member",
                taskSpec = spec,
                providerKey = "mysql",
            ),
        )
    }

    @Test
    fun `aggregate isolate mysql은 task 전용 isolate scope를 사용한다`() {
        val spec = TestcontainersTaskSpec("integrationTest")
        spec.use("mysql")
        spec.isolate("mysql")

        assertEquals(
            ":aggregate:integrationTest:mysql:isolate",
            resolveProviderShareScopeKey(
                projectPath = ":aggregate",
                taskName = "integrationTest",
                executionStackKey = "aggregate",
                taskSpec = spec,
                providerKey = "mysql",
            ),
        )
    }

    @Test
    fun `rabbitmq는 isolate가 없으면 execution stack scope를 유지한다`() {
        val spec = TestcontainersTaskSpec("integrationTest")
        spec.use("rabbitmq")

        assertEquals(
            "transfer",
            resolveProviderShareScopeKey(
                projectPath = ":transfer:application",
                taskName = "integrationTest",
                executionStackKey = "transfer",
                taskSpec = spec,
                providerKey = "rabbitmq",
            ),
        )
    }
}
