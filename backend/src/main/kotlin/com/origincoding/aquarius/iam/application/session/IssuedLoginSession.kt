package com.origincoding.aquarius.iam.application.session

data class IssuedLoginSession(
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long,
)
