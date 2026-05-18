package com.origincoding.aquarius.iam.application.session

fun interface LoginSessionRefresher {
    fun refresh(command: RefreshLoginSessionCommand): RefreshedLoginSession?
}

data class RefreshLoginSessionCommand(
    val refreshToken: String,
)

data class RefreshedLoginSession(
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long,
)
