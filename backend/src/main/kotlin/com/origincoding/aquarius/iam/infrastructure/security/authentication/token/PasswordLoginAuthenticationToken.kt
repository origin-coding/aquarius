package com.origincoding.aquarius.iam.infrastructure.security.authentication.token

import org.springframework.security.authentication.AbstractAuthenticationToken

class PasswordLoginAuthenticationToken private constructor(
    val loginName: String,
    private var rawPassword: String?,
    val captchaChallengeId: String?,
    val captchaCode: String,
) : AbstractAuthenticationToken(emptyList()) {
    init {
        super.setAuthenticated(false)
    }

    override fun getPrincipal(): Any = loginName

    override fun getCredentials(): Any? = rawPassword

    override fun eraseCredentials() {
        super.eraseCredentials()
        rawPassword = null
    }

    override fun setAuthenticated(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            throw IllegalArgumentException("Cannot set this token to trusted")
        }
        super.setAuthenticated(false)
    }

    companion object {
        fun unauthenticated(
            loginName: String, rawPassword: String,
            captchaChallengeId: String?, captchaCode: String,
        ): PasswordLoginAuthenticationToken = PasswordLoginAuthenticationToken(
            loginName = loginName, rawPassword = rawPassword,
            captchaChallengeId = captchaChallengeId, captchaCode = captchaCode,
        )
    }
}
