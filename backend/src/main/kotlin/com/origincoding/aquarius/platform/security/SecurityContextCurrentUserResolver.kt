package com.origincoding.aquarius.platform.security

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserPrincipal
import com.origincoding.aquarius.shared.security.CurrentUserResolver
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component

@Component
@Order(100)
class SecurityContextCurrentUserResolver : CurrentUserResolver {
    override fun resolveCurrentUser(): CurrentUser? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null

        if (!authentication.isAuthenticated) {
            return null
        }

        val authorityNames = authentication.authorities
            .mapNotNull { it.authority }
            .filter { it.isNotBlank() }
            .toSet()

        return when (val principal = authentication.principal) {
            is CurrentUser -> principal.copy(authorities = principal.authorities + authorityNames)
            is CurrentUserPrincipal -> principal.currentUser.copy(
                authorities = principal.currentUser.authorities + authorityNames
            )
            is UserDetails -> CurrentUser(
                id = principal.username,
                username = principal.username,
                authorities = authorityNames
            )
            is String -> principal
                .takeUnless { it == ANONYMOUS_USER }
                ?.let { CurrentUser(id = it, username = authentication.name, authorities = authorityNames) }
            else -> authentication.name
                .takeUnless { it.isBlank() }
                ?.let { CurrentUser(id = it, authorities = authorityNames) }
        }
    }

    private companion object {
        const val ANONYMOUS_USER = "anonymousUser"
    }
}
