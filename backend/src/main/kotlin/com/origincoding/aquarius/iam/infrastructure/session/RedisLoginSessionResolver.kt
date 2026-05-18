package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.application.session.ResolvedLoginSession
import com.origincoding.aquarius.shared.security.CurrentUser
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component

@Component
class RedisLoginSessionResolver(
    private val redissonClient: RedissonClient,
    private val tokenHasher: TokenHasher,
) : LoginSessionResolver {
    override fun resolve(accessToken: String): ResolvedLoginSession? {
        val accessTokenHash = tokenHasher.hash(accessToken)
        val sessionId = redissonClient
            .getBucket<String>(RedisLoginSessionKeys.accessTokenKey(accessTokenHash), StringCodec.INSTANCE)
            .get()
            ?: return null

        val record = redissonClient
            .getBucket<RedisLoginSessionRecord>(RedisLoginSessionKeys.sessionKey(sessionId))
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
