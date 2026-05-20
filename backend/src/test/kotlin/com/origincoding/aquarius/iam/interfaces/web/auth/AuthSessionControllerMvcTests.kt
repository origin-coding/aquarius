package com.origincoding.aquarius.iam.interfaces.web.auth

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.LoginSessionRevoker
import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import com.origincoding.aquarius.iam.application.session.RevokeAllLoginSessionsCommand
import com.origincoding.aquarius.iam.application.session.RevokeLoginSessionCommand
import com.origincoding.aquarius.iam.interfaces.web.IamResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserProvider
import com.origincoding.aquarius.shared.web.error.GlobalControllerAdvice
import com.origincoding.aquarius.shared.web.error.GlobalResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AuthSessionController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalControllerAdvice::class,
    ResultCodeHttpStatusRegistry::class,
    GlobalResultCodeHttpStatusMapper::class,
    IamResultCodeHttpStatusMapper::class,
)
class AuthSessionControllerMvcTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var loginSessionRefresher: LoginSessionRefresher

    @MockitoBean
    private lateinit var loginSessionRevoker: LoginSessionRevoker

    @MockitoBean
    private lateinit var loginSessionResolver: LoginSessionResolver

    @MockitoBean
    private lateinit var currentUserProvider: CurrentUserProvider

    @BeforeEach
    fun stubCurrentUser() {
        `when`(currentUserProvider.currentUser()).thenReturn(CurrentUser(id = "user-id"))
    }

    @Test
    fun `refreshes session through HTTP contract`() {
        val refreshedSession = RefreshedLoginSession(
            sessionId = "session-id",
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 900,
            refreshExpiresIn = 2_592_000,
        )
        `when`(loginSessionRefresher.refresh(RefreshLoginSessionCommand("refresh-token")))
            .thenReturn(refreshedSession)

        mockMvc.perform(
            post("/iam/auth/sessions/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"refresh-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.OK.code))
            .andExpect(jsonPath("$.data.sessionId").value("session-id"))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(900))
            .andExpect(jsonPath("$.data.refreshExpiresIn").value(2_592_000))

        verify(loginSessionRefresher).refresh(RefreshLoginSessionCommand("refresh-token"))
    }

    @Test
    fun `maps blank refresh token to unauthorized error response`() {
        mockMvc.perform(
            post("/iam/auth/sessions/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"   "}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(IamAuthResultCode.INVALID_REFRESH_TOKEN.code))

        verifyNoInteractions(loginSessionRefresher)
    }

    @Test
    fun `maps malformed refresh request body to bad request error response`() {
        mockMvc.perform(
            post("/iam/auth/sessions/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.REQUEST_MALFORMED.code))

        verifyNoInteractions(loginSessionRefresher)
    }

    @Test
    fun `logs out current session through HTTP contract`() {
        `when`(loginSessionRevoker.revoke(RevokeLoginSessionCommand("access-token"))).thenReturn(true)

        mockMvc.perform(
            delete("/iam/auth/sessions/current")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.OK.code))

        verify(loginSessionRevoker).revoke(RevokeLoginSessionCommand("access-token"))
    }

    @Test
    fun `maps non bearer logout header to unauthorized error response`() {
        mockMvc.perform(
            delete("/iam/auth/sessions/current")
                .header(HttpHeaders.AUTHORIZATION, "Basic credentials")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value(IamAuthResultCode.UNAUTHENTICATED.code))

        verifyNoInteractions(loginSessionRevoker)
    }

    @Test
    fun `logs out all sessions for current user through HTTP contract`() {
        mockMvc.perform(delete("/iam/auth/sessions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.OK.code))

        verify(loginSessionRevoker).revokeAll(RevokeAllLoginSessionsCommand("user-id"))
    }
}
