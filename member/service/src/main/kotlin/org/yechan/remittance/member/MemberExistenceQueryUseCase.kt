package org.yechan.remittance.member

fun interface MemberExistenceQueryUseCase {
    fun exists(memberId: Long): Boolean
}

class MemberExistenceQueryService(
    private val memberRepository: MemberRepository,
) : MemberExistenceQueryUseCase {
    override fun exists(memberId: Long): Boolean = memberRepository.findById(MemberId(memberId)) != null

    private data class MemberId(
        override val memberId: Long?,
    ) : MemberIdentifier
}
