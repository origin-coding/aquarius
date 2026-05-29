package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionRefresher
import com.origincoding.aquarius.iam.application.session.RefreshLoginSessionCommand
import com.origincoding.aquarius.iam.application.session.RefreshedLoginSession
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RedisLoginSessionRefresher(
    private val sessionStore: RedisLoginSessionStore,
    private val tokenGenerator: SecureOpaqueTokenGenerator,
    private val tokenHasher: TokenHasher,
    private val properties: IamSessionProperties,
) : LoginSessionRefresher {
    override fun refresh(command: RefreshLoginSessionCommand): RefreshedLoginSession? {
        val currentRefreshTokenHash = tokenHasher.hash(command.refreshToken)
        val sessionId = sessionStore
            .refreshTokenBucket(currentRefreshTokenHash)
            .get()
            ?: return null

        val sessionBucket = sessionStore.sessionBucket(sessionId)
        val currentRecord = sessionBucket.get() ?: return null
        val remainingRefreshTtl = remainingRefreshTtl(currentRecord.refreshExpiresAt)
            .takeIf { !it.isZero }
            ?: return null

        val newAccessToken = tokenGenerator.generate()
        val newRefreshToken = tokenGenerator.generate(REFRESH_TOKEN_BYTE_LENGTH)
        val newAccessTokenHash = tokenHasher.hash(newAccessToken)
        val newRefreshTokenHash = tokenHasher.hash(newRefreshToken)
        val refreshedRecord = currentRecord.copy(
            accessTokenHash = newAccessTokenHash,
            refreshTokenHash = newRefreshTokenHash,
        )

        // TODO: Make refresh rotation atomic before supporting high-concurrency or replay-sensitive deployments.
        // This delete-then-set sequence can race when the same refresh token is submitted concurrently.
        sessionStore
            .accessTokenBucket(currentRecord.accessTokenHash)
            .delete()
        sessionStore
            .refreshTokenBucket(currentRecord.refreshTokenHash)
            .delete()
        sessionStore
            .accessTokenBucket(newAccessTokenHash)
            .set(sessionId, properties.accessTokenTtl)
        sessionStore
            .refreshTokenBucket(newRefreshTokenHash)
            .set(sessionId, remainingRefreshTtl)
        sessionBucket.set(refreshedRecord, remainingRefreshTtl)

        return RefreshedLoginSession(
            sessionId = sessionId,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = properties.accessTokenTtl.toSeconds(),
            refreshExpiresIn = remainingRefreshTtl.toSeconds(),
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
