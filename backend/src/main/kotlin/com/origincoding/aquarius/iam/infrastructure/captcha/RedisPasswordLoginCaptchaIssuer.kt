package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaIssuer
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.IssuedCaptcha
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@ConditionalOnProperty(
    prefix = "aquarius.iam.captcha.password-login",
    name = ["store"],
    havingValue = "redis",
    matchIfMissing = true,
)
class RedisPasswordLoginCaptchaIssuer(
    private val captchaStore: RedisPasswordLoginCaptchaStore,
    private val imageGenerator: PasswordLoginCaptchaImageGenerator,
    private val properties: PasswordLoginCaptchaProperties,
) : CaptchaIssuer {
    private val codeHasher = PasswordLoginCaptchaCodeHasher()

    override fun issue(command: IssueCaptchaCommand): IssuedCaptcha {
        require(command.purpose == CaptchaPurpose.PASSWORD_LOGIN) {
            "Unsupported captcha purpose: ${command.purpose}"
        }

        val now = Instant.now()
        val challengeId = UUID.randomUUID().toString()
        val generatedCaptcha = imageGenerator.generate(properties.codeLength)
        val record = RedisPasswordLoginCaptchaRecord(
            challengeId = challengeId,
            codeHash = codeHasher.hash(challengeId, generatedCaptcha.code),
            delivery = CaptchaDelivery.IMAGE,
            expiresAt = now.plus(properties.ttl),
            createdAt = now,
            attemptCount = 0,
        )

        captchaStore.challengeBucket(challengeId).set(record, properties.ttl)

        return IssuedCaptcha(
            captchaChallengeId = challengeId,
            delivery = CaptchaDelivery.IMAGE,
            expiresIn = properties.ttl.toSeconds(),
            imageBase64 = generatedCaptcha.imageBase64,
            imageContentType = generatedCaptcha.imageContentType,
        )
    }
}
