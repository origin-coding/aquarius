package com.origincoding.aquarius.platform.security

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserContext
import com.origincoding.aquarius.shared.security.CurrentUserPrincipal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class CurrentUserProviderTests {
    private val provider = CompositeCurrentUserProvider(
        listOf(
            ContextCurrentUserResolver(),
            SecurityContextCurrentUserResolver(),
            SystemCurrentUserResolver(),
        )
    )

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `returns system user by default`() {
        assertEquals(CurrentUser.SYSTEM_ID, provider.currentUser().id)
    }

    @Test
    fun `uses current user context before spring security context`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("security-user", null, emptyList())

        CurrentUserContext.runAs(CurrentUser(id = "context-user")) {
            assertEquals("context-user", provider.currentUser().id)
        }
    }

    @Test
    fun `resolves current user principal from spring security context`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                TestCurrentUserPrincipal(CurrentUser(id = "principal-user", username = "alice")),
                null,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
            )

        val currentUser = provider.currentUser()

        assertEquals("principal-user", currentUser.id)
        assertEquals("alice", currentUser.username)
        assertEquals(setOf("ROLE_ADMIN"), currentUser.authorities)
    }

    @Test
    fun `propagates current user context across coroutine dispatcher switch`() = runBlocking {
        CurrentUserContext.runAsSuspend(CurrentUser(id = "coroutine-user")) {
            val currentUser = withContext(Dispatchers.Default) {
                provider.currentUser()
            }

            assertEquals("coroutine-user", currentUser.id)
        }
    }

    private class TestCurrentUserPrincipal(
        override val currentUser: CurrentUser,
    ) : CurrentUserPrincipal
}
