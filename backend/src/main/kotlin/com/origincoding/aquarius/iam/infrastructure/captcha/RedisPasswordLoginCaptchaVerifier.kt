package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.CaptchaVerifier
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
    private val properties: PasswordLoginCaptchaProperties,
) : CaptchaVerifier {
    private val codeHasher = PasswordLoginCaptchaCodeHasher()

    override fun verify(command: VerifyCaptchaCommand) {
        if (command.purpose != CaptchaPurpose.PASSWORD_LOGIN) {
            throw InvalidCaptchaException()
        }

        val challengeId = command.challengeId?.takeIf { it.isNotBlank() }
            ?: throw InvalidCaptchaException()
        val bucket = captchaStore.challengeBucket(challengeId)
        val record = bucket.get() ?: throw InvalidCaptchaException()
        val now = Instant.now()

        if (record.challengeId != challengeId || !record.expiresAt.isAfter(now)) {
            bucket.delete()
            throw InvalidCaptchaException()
        }

        if (codeHasher.matches(challengeId, command.code, record.codeHash)) {
            bucket.delete()
            return
        }

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
