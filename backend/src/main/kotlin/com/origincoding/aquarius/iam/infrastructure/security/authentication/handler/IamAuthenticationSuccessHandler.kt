package com.origincoding.aquarius.iam.infrastructure.security.authentication.handler

import com.origincoding.aquarius.iam.application.session.IssuedLoginSession
import com.origincoding.aquarius.iam.application.session.LoginSessionIssuer
import com.origincoding.aquarius.iam.application.session.LoginSessionPrincipal
import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserPrincipal
import com.origincoding.aquarius.shared.web.response.JsonResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

@Component
class IamAuthenticationSuccessHandler(
    private val loginSessionIssuer: LoginSessionIssuer,
    private val jsonMapper: JsonMapper,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
        authentication: Authentication,
    ) {
        onAuthenticationSuccess(request, response, authentication)
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val currentUser = when (val principal = authentication.principal) {
            is CurrentUserPrincipal -> principal.currentUser
            is CurrentUser -> principal
            else -> throw AuthenticationServiceException("Unsupported authenticated principal")
        }
        val session = loginSessionIssuer.issue(LoginSessionPrincipal(currentUser))

        response.status = HttpStatus.OK.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()

        val jsonResponse = JsonResponse.ok(LoginSuccessResponse(session = session, user = currentUser))
        jsonMapper.writeValue(response.outputStream, jsonResponse)
    }
}

private data class LoginSuccessResponse(
    val session: IssuedLoginSession,
    val user: CurrentUser,
)
