package org.yechan.remittance.account.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ProcessedEventJpaRepositoryTest {
    @Autowired
    lateinit var processedEventRepository: ProcessedEventJpaRepository

    @Test
    fun `처리한 이벤트를 저장하면 event id로 존재 여부를 조회할 수 있다`() {
        processedEventRepository.save(
            ProcessedEventEntity.create(
                100L,
                LocalDateTime.of(2025, 1, 1, 0, 0),
            ),
        )

        assertThat(processedEventRepository.existsByEventId(100L)).isTrue()
    }
}
