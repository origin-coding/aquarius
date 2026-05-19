package com.origincoding.aquarius.iam.application.session

fun interface LoginSessionRevoker {
    fun revoke(command: RevokeLoginSessionCommand): Boolean
}

data class RevokeLoginSessionCommand(
    val accessToken: String,
)
