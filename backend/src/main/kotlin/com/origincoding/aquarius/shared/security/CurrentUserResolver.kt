package com.origincoding.aquarius.shared.security

fun interface CurrentUserResolver {
    fun resolveCurrentUser(): CurrentUser?
}
