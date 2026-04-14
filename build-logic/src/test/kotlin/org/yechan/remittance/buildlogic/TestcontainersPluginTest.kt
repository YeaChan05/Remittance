package org.yechan.remittance.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

class TestcontainersPluginTest {
    @Test
    fun `같은 stack에서는 repository integrationTest가 application integrationTest보다 먼저 오도록 pair를 만든다`() {
        val taskRegistrations = listOf(
            SharedContainerTaskRegistration(
                projectPath = ":member:repository-jpa",
                taskName = "integrationTest",
                executionStackKey = "member",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
            SharedContainerTaskRegistration(
                projectPath = ":member:application",
                taskName = "integrationTest",
                executionStackKey = "member",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
            SharedContainerTaskRegistration(
                projectPath = ":transfer:repository-jpa",
                taskName = "integrationTest",
                executionStackKey = "transfer",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
            SharedContainerTaskRegistration(
                projectPath = ":transfer:application",
                taskName = "integrationTest",
                executionStackKey = "transfer",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
        )

        assertEquals(
            setOf(
                ":member:repository-jpa:integrationTest" to ":member:application:integrationTest",
                ":transfer:repository-jpa:integrationTest" to ":transfer:application:integrationTest",
            ),
            repositoryBeforeApplicationPairs(taskRegistrations).toSet(),
        )
    }

    @Test
    fun `repository integrationTest가 없으면 ordering pair를 만들지 않는다`() {
        val taskRegistrations = listOf(
            SharedContainerTaskRegistration(
                projectPath = ":member:application",
                taskName = "integrationTest",
                executionStackKey = "member",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
        )

        assertEquals(
            emptyList(),
            repositoryBeforeApplicationPairs(taskRegistrations),
        )
    }

    @Test
    fun `같은 provider share scope라도 execution stack이 다르면 ordering pair를 만들지 않는다`() {
        val taskRegistrations = listOf(
            SharedContainerTaskRegistration(
                projectPath = ":member:repository-jpa",
                taskName = "integrationTest",
                executionStackKey = "member",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
            SharedContainerTaskRegistration(
                projectPath = ":account:application",
                taskName = "integrationTest",
                executionStackKey = "account",
                providerShareScopeKeys = mapOf("mysql" to "mysql:non-aggregate"),
            ),
        )

        assertEquals(
            emptyList(),
            repositoryBeforeApplicationPairs(taskRegistrations),
        )
    }
}
