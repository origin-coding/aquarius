package com.origincoding.aquarius.iam.application.session

import com.origincoding.aquarius.shared.security.CurrentUser

fun interface LoginSessionResolver {
    fun resolve(accessToken: String): ResolvedLoginSession?
}

data class ResolvedLoginSession(
    val sessionId: String,
    val user: CurrentUser,
)
