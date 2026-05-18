package com.origincoding.aquarius.iam.application.auth

import com.origincoding.aquarius.iam.domain.model.IdentityType

data class NormalizedLoginName(
    val identityType: IdentityType,
    val normalizedIdentity: String,
)

interface LoginNameNormalizer {
    fun normalize(loginName: String): NormalizedLoginName?
}
