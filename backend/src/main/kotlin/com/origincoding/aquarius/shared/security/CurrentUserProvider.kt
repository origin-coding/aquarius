package com.origincoding.aquarius.shared.security

fun interface CurrentUserProvider {
    fun currentUser(): CurrentUser
}
