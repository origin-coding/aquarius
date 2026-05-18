package com.origincoding.aquarius.iam.application.session

fun interface LoginSessionIssuer {
    fun issue(principal: LoginSessionPrincipal): IssuedLoginSession
}
