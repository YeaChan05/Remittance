package org.yechan.remittance.member

data class Member(
    override val memberId: Long?,
    override val name: String,
    override val email: String,
    override val password: String
) : MemberModel
