package com.origincoding.aquarius.iam.infrastructure.security.authentication

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.IssuedLoginSession
import com.origincoding.aquarius.iam.application.session.LoginSessionIssuer
import com.origincoding.aquarius.iam.application.session.LoginSessionPrincipal
import com.origincoding.aquarius.iam.domain.model.IdentityType
import com.origincoding.aquarius.iam.infrastructure.security.authentication.converter.PasswordLoginAuthenticationConverter
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationFailureHandler
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationSuccessHandler
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.IamAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.PasswordLoginAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.principal.IamAuthenticatedPrincipal
import com.origincoding.aquarius.iam.interfaces.web.IamResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.web.error.GlobalResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import jakarta.servlet.Filter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationFilter
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import tools.jackson.module.kotlin.jacksonMapperBuilder

class PasswordLoginAuthenticationFilterMvcTests {
    private val authenticationProvider = mock(AuthenticationProvider::class.java)
    private val loginSessionIssuer = mock(LoginSessionIssuer::class.java)
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        `when`(authenticationProvider.supports(PasswordLoginAuthenticationToken::class.java)).thenReturn(true)

        val jsonMapper = jacksonMapperBuilder().build()
        val resultCodeHttpStatusRegistry = ResultCodeHttpStatusRegistry(
            listOf(
                GlobalResultCodeHttpStatusMapper(),
                IamResultCodeHttpStatusMapper(),
            )
        )
        val passwordLoginAuthenticationConverter = PasswordLoginAuthenticationConverter(jsonMapper)
        val authenticationFilter = AuthenticationFilter(
            ProviderManager(listOf(authenticationProvider)),
            DelegatingAuthenticationConverter(listOf(passwordLoginAuthenticationConverter)),
        ).apply {
            requestMatcher = passwordLoginAuthenticationConverter.requestMatcher
            successHandler = IamAuthenticationSuccessHandler(loginSessionIssuer, jsonMapper)
            failureHandler = IamAuthenticationFailureHandler(jsonMapper, resultCodeHttpStatusRegistry)
        }

        val mockMvcBuilder: StandaloneMockMvcBuilder = MockMvcBuilders.standaloneSetup(NoHandlerController())
        mockMvc = mockMvcBuilder
            .addFilters<StandaloneMockMvcBuilder>(authenticationFilter as Filter)
            .build()
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `logs in with password through authentication filter`() {
        val currentUser = CurrentUser(id = "user-id", username = "alice", name = "Alice")
        val authentication = IamAuthenticationToken(
            IamAuthenticatedPrincipal(
                userId = "user-id",
                identityId = "identity-id",
                identityType = IdentityType.USERNAME,
                identity = "alice",
                displayName = "Alice",
            )
        )
        val issuedSession = IssuedLoginSession(
            sessionId = "session-id",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 900,
            refreshExpiresIn = 2_592_000,
        )
        `when`(authenticationProvider.authenticate(any(Authentication::class.java))).thenReturn(authentication)
        `when`(loginSessionIssuer.issue(LoginSessionPrincipal(currentUser))).thenReturn(issuedSession)

        mockMvc.perform(
            post("/iam/auth/sessions/password")
                .servletPath("/iam/auth/sessions/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "loginName": "alice",
                      "password": "correct-password",
                      "captchaChallengeId": "challenge-id",
                      "captchaCode": "8888"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.OK.code))
            .andExpect(jsonPath("$.data.session.sessionId").value("session-id"))
            .andExpect(jsonPath("$.data.session.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.session.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.session.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.user.id").value("user-id"))
            .andExpect(jsonPath("$.data.user.username").value("alice"))

        val authenticationCaptor = ArgumentCaptor.forClass(Authentication::class.java)
        verify(authenticationProvider).authenticate(authenticationCaptor.capture())
        val passwordLoginToken = authenticationCaptor.value as PasswordLoginAuthenticationToken
        assertEquals("alice", passwordLoginToken.loginName)
        assertEquals("correct-password", passwordLoginToken.credentials)
        assertEquals("challenge-id", passwordLoginToken.captchaChallengeId)
        assertEquals("8888", passwordLoginToken.captchaCode)
        verify(loginSessionIssuer).issue(LoginSessionPrincipal(currentUser))
    }

    @Test
    fun `maps bad password login credentials to unauthorized error response`() {
        `when`(authenticationProvider.authenticate(any(Authentication::class.java)))
            .thenThrow(BadCredentialsException("Invalid login name or password"))

        mockMvc.perform(
            post("/iam/auth/sessions/password")
                .servletPath("/iam/auth/sessions/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "loginName": "alice",
                      "password": "wrong-password",
                      "captchaChallengeId": "challenge-id",
                      "captchaCode": "8888"
                    }
                    """.trimIndent()
                )
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(IamAuthResultCode.INVALID_CREDENTIALS.code))

        verifyNoInteractions(loginSessionIssuer)
    }

    @Test
    fun `maps malformed password login request to bad request error response`() {
        mockMvc.perform(
            post("/iam/auth/sessions/password")
                .servletPath("/iam/auth/sessions/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"loginName":"alice","password":""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.REQUEST_MALFORMED.code))

        verifyNoInteractions(authenticationProvider, loginSessionIssuer)
    }

    private class NoHandlerController
}
