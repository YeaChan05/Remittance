package org.yechan.remittance.buildlogic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedContainerStackTrackerTest {
    @Test
    fun `같은 stack의 마지막 integrationTest가 끝날 때만 close 신호를 반환한다`() {
        val tracker = SharedContainerStackTracker()

        tracker.register(
            "member",
            setOf(
                ":member:repository-jpa:integrationTest",
                ":member:application:integrationTest",
            ),
        )

        assertFalse(tracker.release("member", ":member:repository-jpa:integrationTest"))
        assertTrue(tracker.release("member", ":member:application:integrationTest"))
    }

    @Test
    fun `선택되지 않은 task release는 무시한다`() {
        val tracker = SharedContainerStackTracker()

        tracker.register(
            "member",
            setOf(":member:application:integrationTest"),
        )

        assertFalse(tracker.release("member", ":member:repository-jpa:integrationTest"))
        assertTrue(tracker.release("member", ":member:application:integrationTest"))
    }
}
