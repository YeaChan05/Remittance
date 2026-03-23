package org.yechan.remittance.member.repository

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.yechan.remittance.member.MemberProps
import org.yechan.remittance.member.member.repository.TestApplication

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemberRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemberJpaRepositoryTest {
    @Autowired
    lateinit var memberRepository: MemberJpaRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `회원 엔티티를 저장하면 다시 조회할 수 있다`() {
        val member = MemberEntity.create(
            object : MemberProps {
                override val name: String = "name"
                override val email: String = "test@test.com"
                override val password: String = "qweasdqwe"
            },
        )

        val saved = memberRepository.save(member)
        entityManager.flush()

        assertThat(saved.memberId).isNotNull()
        val byId = memberRepository.findById(saved.memberId!!)
        assertThat(byId).isPresent
        assertThat(byId.get()).isEqualTo(saved)
    }
}
