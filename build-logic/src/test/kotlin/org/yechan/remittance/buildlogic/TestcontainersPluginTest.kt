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
                stackKey = "member",
            ),
            SharedContainerTaskRegistration(
                projectPath = ":member:application",
                taskName = "integrationTest",
                stackKey = "member",
            ),
            SharedContainerTaskRegistration(
                projectPath = ":transfer:repository-jpa",
                taskName = "integrationTest",
                stackKey = "transfer",
            ),
            SharedContainerTaskRegistration(
                projectPath = ":transfer:application",
                taskName = "integrationTest",
                stackKey = "transfer",
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
                stackKey = "member",
            ),
        )

        assertEquals(
            emptyList(),
            repositoryBeforeApplicationPairs(taskRegistrations),
        )
    }
}
