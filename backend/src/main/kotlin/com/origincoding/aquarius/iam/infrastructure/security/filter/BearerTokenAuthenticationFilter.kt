package com.origincoding.aquarius.iam.infrastructure.security.filter

import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.IamAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.principal.SessionAuthenticatedPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class BearerTokenAuthenticationFilter(
    private val loginSessionResolver: LoginSessionResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val accessToken = request.bearerToken()
        if (accessToken == null) {
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val session = loginSessionResolver.resolve(accessToken)
        if (session == null) {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = IamAuthenticationToken(
            SessionAuthenticatedPrincipal(session.user)
        )
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.bearerToken(): String? {
        val authorization = getHeader(AUTHORIZATION_HEADER)?.trim()
            ?: return null

        if (!authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            return null
        }

        return authorization.substring(BEARER_PREFIX.length)
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
