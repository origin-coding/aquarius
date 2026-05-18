package com.origincoding.aquarius.iam.infrastructure.security.authentication.token

import com.origincoding.aquarius.shared.security.CurrentUserPrincipal
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class IamAuthenticationToken(
    private val authenticatedPrincipal: CurrentUserPrincipal,
    authorities: Collection<GrantedAuthority> = emptyList(),
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(true)
    }

    override fun getPrincipal(): Any = authenticatedPrincipal

    override fun getCredentials(): Any? = null

    override fun setAuthenticated(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            throw IllegalArgumentException("Cannot set this token to trusted")
        }
        super.setAuthenticated(false)
    }
}
