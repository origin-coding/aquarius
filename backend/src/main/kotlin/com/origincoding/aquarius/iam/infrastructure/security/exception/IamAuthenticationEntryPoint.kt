package com.origincoding.aquarius.iam.infrastructure.security.exception

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import com.origincoding.aquarius.shared.web.response.JsonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

@Component
class IamAuthenticationEntryPoint(
    private val jsonMapper: JsonMapper,
    private val resultCodeHttpStatusRegistry: ResultCodeHttpStatusRegistry,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = resultCodeHttpStatusRegistry.getStatus(IamAuthResultCode.UNAUTHENTICATED).value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, JsonResponse.error(IamAuthResultCode.UNAUTHENTICATED))
    }
}
