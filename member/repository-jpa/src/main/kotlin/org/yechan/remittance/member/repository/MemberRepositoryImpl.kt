package org.yechan.remittance.member.repository

import org.yechan.remittance.member.MemberIdentifier
import org.yechan.remittance.member.MemberModel
import org.yechan.remittance.member.MemberProps
import org.yechan.remittance.member.MemberRepository

class MemberRepositoryImpl(
    private val repository: MemberJpaRepository
) : MemberRepository {
    override fun save(props: MemberProps): MemberModel {
        return repository.save(MemberEntity.create(props))
    }

    override fun findById(identifier: MemberIdentifier): MemberModel? {
        val memberId = identifier.memberId ?: return null
        return repository.findById(memberId).orElse(null)
    }

    override fun findByEmail(email: String): MemberModel? {
        return repository.findByEmail(email)
    }
}
