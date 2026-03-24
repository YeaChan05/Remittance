package org.yechan.remittance.member.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.member.MemberModel
import org.yechan.remittance.member.MemberProps

@Entity
@Table(name = "member", catalog = "core")
open class MemberEntity protected constructor() :
    BaseEntity(),
    MemberModel {
    @field:Column(nullable = false)
    override var name: String = ""
        protected set

    @field:Column(nullable = false, unique = true)
    override var email: String = ""
        protected set

    @field:Column(nullable = false)
    private var passwordHash: String = ""

    private constructor(
        name: String,
        email: String,
        passwordHash: String,
    ) : this() {
        this.name = name
        this.email = email
        this.passwordHash = passwordHash
    }

    override val memberId: Long?
        get() = id

    override val password: String
        get() = passwordHash

    companion object {
        fun create(props: MemberProps): MemberEntity = MemberEntity(props.name, props.email, props.password)
    }
}
