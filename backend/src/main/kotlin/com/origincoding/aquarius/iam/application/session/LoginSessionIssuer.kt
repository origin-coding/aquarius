package com.origincoding.aquarius.iam.application.session

import com.origincoding.aquarius.shared.security.CurrentUser

fun interface LoginSessionIssuer {
    fun issue(principal: LoginSessionPrincipal): IssuedLoginSession
}

data class LoginSessionPrincipal(
    val user: CurrentUser,
)

data class IssuedLoginSession(
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long,
)
