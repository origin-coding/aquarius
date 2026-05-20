package com.origincoding.aquarius.iam.interfaces.web.auth

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.LoginSessionRevoker
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import com.origincoding.aquarius.iam.application.session.RevokeAllLoginSessionsCommand
import com.origincoding.aquarius.iam.application.session.RevokeLoginSessionCommand
import com.origincoding.aquarius.shared.error.BusinessException
import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.web.response.JsonResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthSessionControllerTests {
    private val loginSessionRefresher = RecordingLoginSessionRefresher()
    private val loginSessionRevoker = RecordingLoginSessionRevoker()
    private val controller = AuthSessionController(
        loginSessionRefresher = loginSessionRefresher,
        loginSessionRevoker = loginSessionRevoker,
        currentUserProvider = { CurrentUser(id = "user-id") },
    )

    @Test
    fun `refreshes login session with refresh token`() {
        val refreshedSession = RefreshedLoginSession(
            sessionId = "session-id",
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 900,
            refreshExpiresIn = 2_592_000,
        )
        loginSessionRefresher.result = refreshedSession

        val response = controller.refreshToken(RefreshTokenRequest(refreshToken = "refresh-token"))

        assertEquals(RefreshLoginSessionCommand("refresh-token"), loginSessionRefresher.receivedCommand)
        assertSame(refreshedSession, (response as JsonResponse.WithData<*>).data)
    }

    @Test
    fun `rejects blank refresh token`() {
        val exception = assertThrows<BusinessException> {
            controller.refreshToken(RefreshTokenRequest(refreshToken = "   "))
        }

        assertEquals(IamAuthResultCode.INVALID_REFRESH_TOKEN, exception.resultCode)
        assertNull(loginSessionRefresher.receivedCommand)
    }

    @Test
    fun `rejects unresolved refresh token`() {
        val exception = assertThrows<BusinessException> {
            controller.refreshToken(RefreshTokenRequest(refreshToken = "expired-refresh-token"))
        }

        assertEquals(IamAuthResultCode.INVALID_REFRESH_TOKEN, exception.resultCode)
        assertEquals(RefreshLoginSessionCommand("expired-refresh-token"), loginSessionRefresher.receivedCommand)
    }

    @Test
    fun `logs out current session with bearer token`() {
        loginSessionRevoker.revokeResult = true

        controller.logoutCurrent("  bearer access-token  ")

        assertEquals(RevokeLoginSessionCommand("access-token"), loginSessionRevoker.receivedRevokeCommand)
    }

    @Test
    fun `rejects logout current request without bearer token`() {
        val exception = assertThrows<BusinessException> {
            controller.logoutCurrent("Basic credentials")
        }

        assertEquals(IamAuthResultCode.UNAUTHENTICATED, exception.resultCode)
        assertNull(loginSessionRevoker.receivedRevokeCommand)
    }

    @Test
    fun `rejects logout current request when token cannot be revoked`() {
        val exception = assertThrows<BusinessException> {
            controller.logoutCurrent("Bearer unknown-token")
        }

        assertEquals(IamAuthResultCode.UNAUTHENTICATED, exception.resultCode)
        assertEquals(RevokeLoginSessionCommand("unknown-token"), loginSessionRevoker.receivedRevokeCommand)
    }

    @Test
    fun `logs out all sessions for current user`() {
        controller.logoutAll()

        assertEquals(RevokeAllLoginSessionsCommand("user-id"), loginSessionRevoker.receivedRevokeAllCommand)
    }

    private class RecordingLoginSessionRefresher : LoginSessionRefresher {
        var receivedCommand: RefreshLoginSessionCommand? = null
        var result: RefreshedLoginSession? = null

        override fun refresh(command: RefreshLoginSessionCommand): RefreshedLoginSession? {
            receivedCommand = command
            return result
        }
    }

    private class RecordingLoginSessionRevoker : LoginSessionRevoker {
        var receivedRevokeCommand: RevokeLoginSessionCommand? = null
        var receivedRevokeAllCommand: RevokeAllLoginSessionsCommand? = null
        var revokeResult = false

        override fun revoke(command: RevokeLoginSessionCommand): Boolean {
            receivedRevokeCommand = command
            return revokeResult
        }

        override fun revokeAll(command: RevokeAllLoginSessionsCommand) {
            receivedRevokeAllCommand = command
        }
    }
}
