package com.origincoding.aquarius.iam.application.session

fun interface LoginSessionResolver {
    fun resolve(accessToken: String): ResolvedLoginSession?
}
