package org.yechan.remittance.member.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.member.MemberModel
import org.yechan.remittance.member.MemberProps

@Entity
@DynamicUpdate
@Table(
    name = "member",
    catalog = "core",
    indexes = [
        Index(name = "idx_member_email", columnList = "email", unique = true),
    ],
)
class MemberEntity() :
    BaseEntity(),
    MemberModel {
    override val memberId: Long?
        get() = id

    @field:Column(nullable = false)
    override var name: String = ""

    @NaturalId
    @field:Column(nullable = false, unique = true)
    override var email: String = ""

    @field:Column(nullable = false)
    private var passwordHash: String = ""

    override val password: String
        get() = passwordHash

    private constructor(
        name: String,
        email: String,
        passwordHash: String,
    ) : this() {
        this.name = name
        this.email = email
        this.passwordHash = passwordHash
    }

    companion object {
        fun create(props: MemberProps): MemberEntity = MemberEntity(props.name, props.email, props.password)
    }
}
