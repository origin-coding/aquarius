package com.origincoding.aquarius.iam.domain.model

import com.origincoding.aquarius.shared.persistence.entity.AggregateRootBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "iam_user")
class IamUser(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(nullable = false, length = 64)
    var name: String,

    var lastLoginAt: Instant? = null,
) : AggregateRootBase()
