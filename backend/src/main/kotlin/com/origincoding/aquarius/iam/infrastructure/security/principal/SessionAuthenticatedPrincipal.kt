package com.origincoding.aquarius.iam.infrastructure.security.principal

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserPrincipal

data class SessionAuthenticatedPrincipal(
    override val currentUser: CurrentUser,
) : CurrentUserPrincipal
