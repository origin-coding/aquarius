package com.origincoding.aquarius.iam.interfaces.web.auth

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.LoginSessionRevoker
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import com.origincoding.aquarius.iam.application.session.RevokeAllLoginSessionsCommand
import com.origincoding.aquarius.iam.application.session.RevokeLoginSessionCommand
import com.origincoding.aquarius.shared.error.BusinessException
import com.origincoding.aquarius.shared.security.CurrentUserProvider
import com.origincoding.aquarius.shared.web.response.JsonResponse
import com.origincoding.aquarius.shared.web.response.WebApiSupport
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/iam/auth")
class AuthSessionController(
    private val loginSessionRefresher: LoginSessionRefresher,
    private val loginSessionRevoker: LoginSessionRevoker,
    private val currentUserProvider: CurrentUserProvider,
) : WebApiSupport {
    @PostMapping("/sessions/refresh-token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): JsonResponse<RefreshedLoginSession> {
        val refreshToken = request.refreshToken
            ?.takeIf { it.isNotBlank() }
            ?: throw BusinessException(IamAuthResultCode.INVALID_REFRESH_TOKEN)

        val refreshedSession = loginSessionRefresher.refresh(
            RefreshLoginSessionCommand(refreshToken)
        ) ?: throw BusinessException(IamAuthResultCode.INVALID_REFRESH_TOKEN)

        return ok(refreshedSession)
    }

    @DeleteMapping("/sessions/current")
    fun logoutCurrent(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): JsonResponse<Unit> {
        val accessToken = authorization.bearerToken()
            ?: throw BusinessException(IamAuthResultCode.UNAUTHENTICATED)

        val revoked = loginSessionRevoker.revoke(RevokeLoginSessionCommand(accessToken))
        if (!revoked) {
            throw BusinessException(IamAuthResultCode.UNAUTHENTICATED)
        }

        return ok()
    }

    @DeleteMapping("/sessions")
    fun logoutAll(): JsonResponse<Unit> {
        val currentUser = currentUserProvider.currentUser()

        loginSessionRevoker.revokeAll(RevokeAllLoginSessionsCommand(currentUser.id))

        return ok()
    }

    private fun String.bearerToken(): String? {
        val authorization = trim()

        if (!authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            return null
        }

        return authorization.substring(BEARER_PREFIX.length)
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}

data class RefreshTokenRequest(
    val refreshToken: String?,
)
