package com.origincoding.aquarius.platform.security

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserProvider
import com.origincoding.aquarius.shared.security.CurrentUserResolver
import org.springframework.stereotype.Component

@Component
class CompositeCurrentUserProvider(
    private val resolvers: List<CurrentUserResolver>,
) : CurrentUserProvider {
    override fun currentUser(): CurrentUser =
        resolvers.firstNotNullOf { it.resolveCurrentUser() }
}
