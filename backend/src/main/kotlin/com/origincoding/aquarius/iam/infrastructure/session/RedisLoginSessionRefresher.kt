package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RedisLoginSessionRefresher(
    private val redissonClient: RedissonClient,
    private val tokenGenerator: SecureOpaqueTokenGenerator,
    private val tokenHasher: TokenHasher,
    private val properties: IamSessionProperties,
) : LoginSessionRefresher {
    override fun refresh(command: RefreshLoginSessionCommand): RefreshedLoginSession? {
        val currentRefreshTokenHash = tokenHasher.hash(command.refreshToken)
        val sessionId = redissonClient
            .getBucket<String>(RedisLoginSessionKeys.refreshTokenKey(currentRefreshTokenHash), StringCodec.INSTANCE)
            .get()
            ?: return null

        val sessionBucket = redissonClient
            .getBucket<RedisLoginSessionRecord>(RedisLoginSessionKeys.sessionKey(sessionId))
        val currentRecord = sessionBucket.get() ?: return null

        val newAccessToken = tokenGenerator.generate()
        val newRefreshToken = tokenGenerator.generate(REFRESH_TOKEN_BYTE_LENGTH)
        val newAccessTokenHash = tokenHasher.hash(newAccessToken)
        val newRefreshTokenHash = tokenHasher.hash(newRefreshToken)
        val refreshedRecord = currentRecord.copy(
            accessTokenHash = newAccessTokenHash,
            refreshTokenHash = newRefreshTokenHash,
        )

        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.accessTokenKey(currentRecord.accessTokenHash), StringCodec.INSTANCE)
            .delete()
        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.refreshTokenKey(currentRecord.refreshTokenHash), StringCodec.INSTANCE)
            .delete()
        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.accessTokenKey(newAccessTokenHash), StringCodec.INSTANCE)
            .set(sessionId, properties.accessTokenTtl)
        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.refreshTokenKey(newRefreshTokenHash), StringCodec.INSTANCE)
            .set(sessionId, remainingRefreshTtl(currentRecord.refreshExpiresAt))
        sessionBucket.set(refreshedRecord, remainingRefreshTtl(currentRecord.refreshExpiresAt))

        return RefreshedLoginSession(
            sessionId = sessionId,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = properties.accessTokenTtl.toSeconds(),
            refreshExpiresIn = remainingRefreshTtl(currentRecord.refreshExpiresAt).toSeconds(),
        )
    }

    private fun remainingRefreshTtl(refreshExpiresAt: Instant): java.time.Duration {
        val duration = java.time.Duration.between(Instant.now(), refreshExpiresAt)
        return duration.takeIf { !it.isNegative && !it.isZero } ?: java.time.Duration.ZERO
    }

    private companion object {
        const val REFRESH_TOKEN_BYTE_LENGTH = 48
    }
}
