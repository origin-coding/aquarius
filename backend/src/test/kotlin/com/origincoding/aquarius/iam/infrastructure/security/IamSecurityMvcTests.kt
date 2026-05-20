package com.origincoding.aquarius.iam.infrastructure.security

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.LoginSessionIssuer
import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.infrastructure.security.authentication.converter.PasswordLoginAuthenticationConverter
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationFailureHandler
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationSuccessHandler
import com.origincoding.aquarius.iam.infrastructure.security.config.IamLoginConfigurer
import com.origincoding.aquarius.iam.infrastructure.security.config.IamSecurityConfiguration
import com.origincoding.aquarius.iam.infrastructure.security.exception.IamAccessDeniedHandler
import com.origincoding.aquarius.iam.infrastructure.security.exception.IamAuthenticationEntryPoint
import com.origincoding.aquarius.iam.infrastructure.security.filter.BearerTokenAuthenticationFilter
import com.origincoding.aquarius.iam.interfaces.web.IamResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.web.error.GlobalResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [IamSecurityMvcTests.ProtectedController::class])
@AutoConfigureMockMvc
@Import(
    PasswordLoginAuthenticationConverter::class,
    IamLoginConfigurer::class,
    IamSecurityConfiguration::class,
    IamAuthenticationSuccessHandler::class,
    IamAuthenticationFailureHandler::class,
    IamAuthenticationEntryPoint::class,
    IamAccessDeniedHandler::class,
    BearerTokenAuthenticationFilter::class,
    ResultCodeHttpStatusRegistry::class,
    GlobalResultCodeHttpStatusMapper::class,
    IamResultCodeHttpStatusMapper::class,
)
class IamSecurityMvcTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var authenticationProvider: AuthenticationProvider

    @MockitoBean
    private lateinit var loginSessionIssuer: LoginSessionIssuer

    @MockitoBean
    private lateinit var loginSessionResolver: LoginSessionResolver

    @Test
    fun `maps unauthenticated protected request to unauthorized error response`() {
        mockMvc.perform(get("/protected/ping"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(IamAuthResultCode.UNAUTHENTICATED.code))

        verifyNoInteractions(authenticationProvider, loginSessionIssuer, loginSessionResolver)
    }

    @Test
    fun `maps unresolved bearer token on protected request to unauthorized error response`() {
        mockMvc.perform(
            get("/protected/ping")
                .header(HttpHeaders.AUTHORIZATION, "Bearer unknown-token")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(IamAuthResultCode.UNAUTHENTICATED.code))

        verify(loginSessionResolver).resolve("unknown-token")
        verifyNoInteractions(authenticationProvider, loginSessionIssuer)
    }

    @RestController
    class ProtectedController {
        @GetMapping("/protected/ping")
        fun ping(): String = "pong"
    }
}
