package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.platform.redis.typedCodec
import org.redisson.api.RBucket
import org.redisson.api.RSetCache
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RedisLoginSessionStore(
    private val redissonClient: RedissonClient,
    objectMapper: ObjectMapper,
) {
    private val loginSessionRecordCodec = objectMapper.typedCodec<RedisLoginSessionRecord>()

    fun accessTokenBucket(accessTokenHash: String): RBucket<String> =
        redissonClient.getBucket(RedisLoginSessionKeys.accessTokenKey(accessTokenHash), StringCodec.INSTANCE)

    fun refreshTokenBucket(refreshTokenHash: String): RBucket<String> =
        redissonClient.getBucket(RedisLoginSessionKeys.refreshTokenKey(refreshTokenHash), StringCodec.INSTANCE)

    fun sessionBucket(sessionId: String): RBucket<RedisLoginSessionRecord> =
        redissonClient.getBucket(RedisLoginSessionKeys.sessionKey(sessionId), loginSessionRecordCodec)

    fun userSessions(userId: String): RSetCache<String> =
        redissonClient.getSetCache(RedisLoginSessionKeys.userSessionsKey(userId), StringCodec.INSTANCE)
}
