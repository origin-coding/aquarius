package com.origincoding.aquarius.iam.application.session

interface LoginSessionRevoker {
    fun revoke(command: RevokeLoginSessionCommand): Boolean

    fun revokeAll(command: RevokeAllLoginSessionsCommand)
}

data class RevokeLoginSessionCommand(
    val accessToken: String,
)

data class RevokeAllLoginSessionsCommand(
    val userId: String,
)
