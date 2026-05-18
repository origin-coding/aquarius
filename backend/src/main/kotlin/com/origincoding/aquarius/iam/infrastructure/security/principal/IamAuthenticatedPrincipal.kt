package com.origincoding.aquarius.iam.infrastructure.security.principal

import com.origincoding.aquarius.iam.domain.model.IdentityType
import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserPrincipal

data class IamAuthenticatedPrincipal(
    val userId: String,
    val identityId: String,
    val identityType: IdentityType,
    val identity: String,
    val displayName: String,
) : CurrentUserPrincipal {
    override val currentUser: CurrentUser = CurrentUser(
        id = userId,
        username = identity,
        name = displayName,
    )
}
