package com.origincoding.aquarius.iam.domain.model

import com.origincoding.aquarius.shared.persistence.entity.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "iam_identity")
class Identity(
    @Column(nullable = false, length = 36)
    var userId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var identityType: IdentityType,

    @Column(nullable = false, length = 320)
    var identity: String,

    @Column(nullable = false, length = 320)
    var normalizedIdentity: String,

    var verifiedAt: Instant? = null,
) : EntityBase()
