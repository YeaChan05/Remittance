package org.yechan.remittance.buildlogic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedContainerScopeTrackerTest {
    @Test
    fun `같은 share scope의 마지막 task가 끝날 때만 close 신호를 반환한다`() {
        val tracker = SharedContainerScopeTracker()
        val owner = SharedContainerLifecycleOwner("mysql", NON_AGGREGATE_MYSQL_SHARE_SCOPE_KEY)

        tracker.register(
            owner = owner,
            taskPaths = setOf(
                ":member:application:integrationTest",
                ":account:application:integrationTest",
            ),
        )

        assertFalse(tracker.release(owner, ":member:application:integrationTest"))
        assertTrue(tracker.release(owner, ":account:application:integrationTest"))
    }

    @Test
    fun `선택되지 않은 task는 share scope close 계산에서 무시한다`() {
        val tracker = SharedContainerScopeTracker()
        val owner = SharedContainerLifecycleOwner("mysql", NON_AGGREGATE_MYSQL_SHARE_SCOPE_KEY)

        tracker.register(
            owner = owner,
            taskPaths = setOf(":member:application:integrationTest"),
        )

        assertFalse(tracker.release(owner, ":account:application:integrationTest"))
        assertTrue(tracker.release(owner, ":member:application:integrationTest"))
    }
}
