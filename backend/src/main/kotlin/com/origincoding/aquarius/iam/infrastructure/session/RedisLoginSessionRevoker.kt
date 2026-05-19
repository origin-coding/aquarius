package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionRevoker
import com.origincoding.aquarius.iam.application.session.RevokeLoginSessionCommand
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component

@Component
class RedisLoginSessionRevoker(
    private val redissonClient: RedissonClient,
    private val tokenHasher: TokenHasher,
) : LoginSessionRevoker {
    override fun revoke(command: RevokeLoginSessionCommand): Boolean {
        val accessTokenHash = tokenHasher.hash(command.accessToken)
        val accessTokenBucket = redissonClient
            .getBucket<String>(RedisLoginSessionKeys.accessTokenKey(accessTokenHash), StringCodec.INSTANCE)
        val sessionId = accessTokenBucket.get() ?: return false

        val sessionBucket = redissonClient
            .getBucket<RedisLoginSessionRecord>(RedisLoginSessionKeys.sessionKey(sessionId))
        val record = sessionBucket.get()

        accessTokenBucket.delete()
        if (record == null) {
            return false
        }

        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.refreshTokenKey(record.refreshTokenHash), StringCodec.INSTANCE)
            .delete()
        sessionBucket.delete()
        redissonClient
            .getSetCache<String>(RedisLoginSessionKeys.userSessionsKey(record.userId), StringCodec.INSTANCE)
            .remove(sessionId)

        return true
    }
}
