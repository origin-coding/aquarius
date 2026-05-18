package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.IssuedLoginSession
import com.origincoding.aquarius.iam.application.session.LoginSessionIssuer
import com.origincoding.aquarius.iam.application.session.LoginSessionPrincipal
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class RedisLoginSessionIssuer(
    private val redissonClient: RedissonClient,
    private val tokenGenerator: SecureOpaqueTokenGenerator,
    private val tokenHasher: TokenHasher,
    private val properties: IamSessionProperties,
) : LoginSessionIssuer {
    override fun issue(principal: LoginSessionPrincipal): IssuedLoginSession {
        val now = Instant.now()
        val sessionId = UUID.randomUUID().toString()
        val accessToken = tokenGenerator.generate()
        val refreshToken = tokenGenerator.generate(REFRESH_TOKEN_BYTE_LENGTH)
        val accessTokenHash = tokenHasher.hash(accessToken)
        val refreshTokenHash = tokenHasher.hash(refreshToken)

        val record = RedisLoginSessionRecord(
            id = sessionId,
            userId = principal.user.id,
            username = principal.user.username,
            name = principal.user.name,
            accessTokenHash = accessTokenHash,
            refreshTokenHash = refreshTokenHash,
            createdAt = now,
            refreshExpiresAt = now.plus(properties.refreshTokenTtl),
        )

        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.accessTokenKey(accessTokenHash), StringCodec.INSTANCE)
            .set(sessionId, properties.accessTokenTtl)
        redissonClient
            .getBucket<String>(RedisLoginSessionKeys.refreshTokenKey(refreshTokenHash), StringCodec.INSTANCE)
            .set(sessionId, properties.refreshTokenTtl)
        redissonClient
            .getBucket<RedisLoginSessionRecord>(RedisLoginSessionKeys.sessionKey(sessionId))
            .set(record, properties.refreshTokenTtl)
        redissonClient
            .getSetCache<String>(RedisLoginSessionKeys.userSessionsKey(principal.user.id), StringCodec.INSTANCE)
            .add(sessionId, properties.refreshTokenTtl.toMillis(), TimeUnit.MILLISECONDS)

        return IssuedLoginSession(
            sessionId = sessionId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = properties.accessTokenTtl.toSeconds(),
            refreshExpiresIn = properties.refreshTokenTtl.toSeconds(),
        )
    }

    private companion object {
        const val REFRESH_TOKEN_BYTE_LENGTH = 48
    }
}
