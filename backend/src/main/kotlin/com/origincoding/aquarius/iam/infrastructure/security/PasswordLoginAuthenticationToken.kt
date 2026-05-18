package com.origincoding.aquarius.iam.infrastructure.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class PasswordLoginAuthenticationToken private constructor(
    private val tokenPrincipal: Any,
    private var tokenCredentials: Any?,
    val loginName: String,
    val clientIp: String?,
    val userAgent: String?,
    authorities: Collection<GrantedAuthority>,
    authenticated: Boolean,
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(authenticated)
    }

    override fun getPrincipal(): Any = tokenPrincipal

    override fun getCredentials(): Any? = tokenCredentials

    override fun eraseCredentials() {
        super.eraseCredentials()
        tokenCredentials = null
    }

    companion object {
        fun unauthenticated(
            loginName: String,
            password: String,
            clientIp: String? = null,
            userAgent: String? = null,
        ): PasswordLoginAuthenticationToken =
            PasswordLoginAuthenticationToken(
                tokenPrincipal = loginName,
                tokenCredentials = password,
                loginName = loginName,
                clientIp = clientIp,
                userAgent = userAgent,
                authorities = emptyList(),
                authenticated = false,
            )

        fun authenticated(
            principal: IamAuthenticatedPrincipal,
            authorities: Collection<GrantedAuthority> = emptyList(),
        ): PasswordLoginAuthenticationToken =
            PasswordLoginAuthenticationToken(
                tokenPrincipal = principal,
                tokenCredentials = null,
                loginName = principal.identity,
                clientIp = null,
                userAgent = null,
                authorities = authorities,
                authenticated = true,
            )
    }
}
