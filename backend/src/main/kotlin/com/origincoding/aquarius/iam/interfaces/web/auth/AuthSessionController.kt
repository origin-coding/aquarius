package com.origincoding.aquarius.iam.interfaces.web.auth

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import com.origincoding.aquarius.shared.error.BusinessException
import com.origincoding.aquarius.shared.web.response.JsonResponse
import com.origincoding.aquarius.shared.web.response.WebApiSupport
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/iam/auth")
class AuthSessionController(
    private val loginSessionRefresher: LoginSessionRefresher,
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
}

data class RefreshTokenRequest(
    val refreshToken: String?,
)
