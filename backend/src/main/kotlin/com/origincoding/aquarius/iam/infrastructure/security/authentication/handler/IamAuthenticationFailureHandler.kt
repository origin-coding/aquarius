package com.origincoding.aquarius.iam.infrastructure.security.authentication.handler

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.error.ResultCode
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import com.origincoding.aquarius.shared.web.response.JsonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

@Component
class IamAuthenticationFailureHandler(
    private val jsonMapper: JsonMapper,
    private val resultCodeHttpStatusRegistry: ResultCodeHttpStatusRegistry,
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val resultCode = exception.toResultCode()

        response.status = resultCodeHttpStatusRegistry.getStatus(resultCode).value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, JsonResponse.error(resultCode))
    }

    private fun AuthenticationException.toResultCode(): ResultCode =
        when (this) {
            is InvalidCaptchaException -> IamAuthResultCode.INVALID_CAPTCHA
            is BadCredentialsException -> IamAuthResultCode.INVALID_CREDENTIALS
            is DisabledException -> IamAuthResultCode.USER_DISABLED
            is AuthenticationServiceException -> GlobalResultCode.REQUEST_MALFORMED
            else -> IamAuthResultCode.AUTHENTICATION_FAILED
        }
}
