package com.origincoding.aquarius.iam.infrastructure.security.authentication.converter

import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.PasswordLoginAuthenticationToken
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

@Component
class PasswordLoginAuthenticationConverter(
    private val jsonMapper: JsonMapper,
) : IamAuthenticationConverter {
    override val requestMatcher: RequestMatcher =
        PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/iam/auth/password-login")

    override fun convert(request: HttpServletRequest): Authentication? {
        if (!requestMatcher.matches(request)) {
            return null
        }

        val body = try {
            jsonMapper.readValue<PasswordLoginRequest>(request.inputStream)
        } catch (ex: Exception) {
            throw AuthenticationServiceException("Login request is malformed or cannot be parsed", ex)
        }

        return PasswordLoginAuthenticationToken.unauthenticated(
            loginName = body.loginName.requireNotBlank("Login name is required"),
            rawPassword = body.password.requireNotBlank("Password is required"),
            captchaChallengeId = body.captchaChallengeId.requireNotBlank("Captcha challenge id is required"),
            captchaCode = body.captchaCode.requireNotBlank("Captcha code is required"),
        )
    }

    private fun String?.requireNotBlank(message: String): String =
        this?.takeIf { it.isNotBlank() }
            ?: throw AuthenticationServiceException(message)
}

private data class PasswordLoginRequest(
    val loginName: String?,
    val password: String?,
    val captchaChallengeId: String?,
    val captchaCode: String?,
)
