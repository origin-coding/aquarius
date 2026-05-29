package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.CaptchaVerifier
import com.origincoding.aquarius.iam.application.auth.LoginNameNormalizer
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.redisson.api.RBucket
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty(
    prefix = "aquarius.iam.captcha.password-login",
    name = ["store"],
    havingValue = "redis",
    matchIfMissing = true,
)
class RedisPasswordLoginCaptchaVerifier(
    private val captchaStore: RedisPasswordLoginCaptchaStore,
    private val loginNameNormalizer: LoginNameNormalizer,
    private val properties: PasswordLoginCaptchaProperties,
) : CaptchaVerifier {
    private val codeHasher = PasswordLoginCaptchaCodeHasher()
    private val targetHasher = PasswordLoginCaptchaTargetHasher()

    override fun verify(command: VerifyCaptchaCommand) {
        if (command.purpose != CaptchaPurpose.PASSWORD_LOGIN) {
            throw InvalidCaptchaException()
        }

        val challengeId = command.challengeId?.takeIf { it.isNotBlank() }
            ?: throw InvalidCaptchaException()
        val bucket = captchaStore.challengeBucket(challengeId)
        val record = bucket.get() ?: throw InvalidCaptchaException()
        val now = Instant.now()
        val normalizedLoginName = command.target
            ?.let(loginNameNormalizer::normalize)
            ?: throw InvalidCaptchaException()

        if (record.challengeId != challengeId || !record.expiresAt.isAfter(now)) {
            bucket.delete()
            throw InvalidCaptchaException()
        }

        if (record.targetHash == null) {
            bucket.delete()
            throw InvalidCaptchaException()
        }

        if (record.targetHash != targetHasher.hash(normalizedLoginName)) {
            rejectAndTrackFailedAttempt(bucket, record, now)
        }

        if (codeHasher.matches(challengeId, command.code, record.codeHash)) {
            bucket.delete()
            return
        }

        rejectAndTrackFailedAttempt(bucket, record, now)
    }

    private fun rejectAndTrackFailedAttempt(
        bucket: RBucket<RedisPasswordLoginCaptchaRecord>,
        record: RedisPasswordLoginCaptchaRecord,
        now: Instant,
    ): Nothing {
        val failedAttemptCount = record.attemptCount + 1
        if (failedAttemptCount >= properties.maxAttempts) {
            bucket.delete()
        } else {
            val remainingTtl = Duration.between(now, record.expiresAt)
            if (remainingTtl.isPositive) {
                bucket.set(record.copy(attemptCount = failedAttemptCount), remainingTtl)
            } else {
                bucket.delete()
            }
        }

        throw InvalidCaptchaException()
    }
}
