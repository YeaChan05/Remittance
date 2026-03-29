package org.yechan.remittance.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

class MySqlSharedContainerProviderTest {
    @Test
    fun `task path를 task별 mysql database 이름으로 변환한다`() {
        assertEquals(
            "member_repository_jpa_integrationtest",
            MySqlSharedContainerProvider.databaseNameFor(":member:repository-jpa:integrationTest"),
        )
    }

    @Test
    fun `database 이름은 mysql 제한 길이 이내로 자른다`() {
        val databaseName = MySqlSharedContainerProvider.databaseNameFor(
            ":very-long-domain-name:repository-jpa:integrationTestWithVeryLongSuffix",
        )

        assertEquals(64, databaseName.length)
    }
}
