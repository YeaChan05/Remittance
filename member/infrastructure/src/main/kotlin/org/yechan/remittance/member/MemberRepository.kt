package org.yechan.remittance.member

interface MemberRepository {
    fun save(props: MemberProps): MemberModel

    fun findById(identifier: MemberIdentifier): MemberModel?

    fun findByEmail(email: String): MemberModel?
}
