package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.application.session.ResolvedLoginSession
import com.origincoding.aquarius.shared.security.CurrentUser
import org.springframework.stereotype.Component

@Component
class RedisLoginSessionResolver(
    private val sessionStore: RedisLoginSessionStore,
    private val tokenHasher: TokenHasher,
) : LoginSessionResolver {
    override fun resolve(accessToken: String): ResolvedLoginSession? {
        val accessTokenHash = tokenHasher.hash(accessToken)
        val sessionId = sessionStore
            .accessTokenBucket(accessTokenHash)
            .get()
            ?: return null

        val record = sessionStore
            .sessionBucket(sessionId)
            .get()
            ?: return null

        return ResolvedLoginSession(
            sessionId = record.id,
            user = CurrentUser(
                id = record.userId,
                username = record.username,
                name = record.name,
            ),
        )
    }
}
