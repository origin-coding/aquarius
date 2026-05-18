package com.origincoding.aquarius.iam.application.session

import com.origincoding.aquarius.shared.security.CurrentUser

data class ResolvedLoginSession(
    val sessionId: String,
    val user: CurrentUser,
)
