package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaIssuer
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.IssuedCaptcha
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
@ConditionalOnProperty(
    prefix = "aquarius.iam.captcha.password-login",
    name = ["store"],
    havingValue = "fixed-local",
)
class FixedLocalCaptchaIssuer : CaptchaIssuer {
    private val logger = KotlinLogging.logger { }

    override fun issue(command: IssueCaptchaCommand): IssuedCaptcha {
        require(command.purpose == CaptchaPurpose.PASSWORD_LOGIN) {
            "Unsupported captcha purpose: ${command.purpose}"
        }

        logger.info {
            "Issued local captcha: purpose=${command.purpose}, challengeId=$CHALLENGE_ID, code=$CODE"
        }

        return IssuedCaptcha(
            captchaChallengeId = CHALLENGE_ID,
            delivery = CaptchaDelivery.LOCAL,
        )
    }

    private companion object {
        const val CHALLENGE_ID = "local"
        const val CODE = "8888"
    }
}
