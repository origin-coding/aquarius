package com.origincoding.aquarius.iam.infrastructure.security.filter

import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.application.session.ResolvedLoginSession
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.IamAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.principal.SessionAuthenticatedPrincipal
import com.origincoding.aquarius.shared.security.CurrentUser
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class BearerTokenAuthenticationFilterTests {
    private val loginSessionResolver = RecordingLoginSessionResolver()
    private val filter = BearerTokenAuthenticationFilter(loginSessionResolver)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `continues filter chain without authorization header`() {
        val chain = RecordingFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertEquals(1, chain.invocations)
        assertNull(loginSessionResolver.receivedAccessToken)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `continues filter chain without resolving non bearer authorization header`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic credentials")
        }
        val chain = RecordingFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals(1, chain.invocations)
        assertNull(loginSessionResolver.receivedAccessToken)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `authenticates resolved bearer token`() {
        val currentUser = CurrentUser(id = "user-id", username = "alice")
        loginSessionResolver.result = ResolvedLoginSession(
            sessionId = "session-id",
            user = currentUser,
        )
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "  bearer access-token  ")
        }
        val chain = RecordingFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertEquals(1, chain.invocations)
        assertEquals("access-token", loginSessionResolver.receivedAccessToken)
        assertNotNull(authentication)
        assertTrue(authentication is IamAuthenticationToken)
        assertTrue(authentication!!.isAuthenticated)
        assertSame(currentUser, (authentication.principal as SessionAuthenticatedPrincipal).currentUser)
    }

    @Test
    fun `leaves security context empty when bearer token cannot be resolved`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer unknown-token")
        }
        val chain = RecordingFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals(1, chain.invocations)
        assertEquals("unknown-token", loginSessionResolver.receivedAccessToken)
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `does not replace existing authentication`() {
        val existingAuthentication = UsernamePasswordAuthenticationToken("existing-user", null)
        SecurityContextHolder.getContext().authentication = existingAuthentication
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer access-token")
        }
        val chain = RecordingFilterChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertEquals(1, chain.invocations)
        assertNull(loginSessionResolver.receivedAccessToken)
        assertSame(existingAuthentication, SecurityContextHolder.getContext().authentication)
    }

    private class RecordingLoginSessionResolver : LoginSessionResolver {
        var receivedAccessToken: String? = null
        var result: ResolvedLoginSession? = null

        override fun resolve(accessToken: String): ResolvedLoginSession? {
            receivedAccessToken = accessToken
            return result
        }
    }

    private class RecordingFilterChain : FilterChain {
        var invocations = 0

        override fun doFilter(request: ServletRequest, response: ServletResponse) {
            invocations += 1
        }
    }
}
