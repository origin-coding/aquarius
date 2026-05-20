package com.origincoding.aquarius.iam.domain.model

import com.origincoding.aquarius.shared.persistence.entity.EntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "iam_credential")
class Credential(
    @Column(nullable = false, length = 36)
    var identityId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var credentialType: CredentialType,

    @Column(nullable = false, length = 255)
    var secret: String,
) : EntityBase()
