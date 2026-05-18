package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.CaptchaVerifier
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class FixedLocalCaptchaVerifier : CaptchaVerifier {
    override fun verify(command: VerifyCaptchaCommand) {
        if (command.purpose != CaptchaPurpose.PASSWORD_LOGIN) {
            throw InvalidCaptchaException()
        }

        if (command.challengeId != "local" || command.code != "8888") {
            throw InvalidCaptchaException()
        }
    }
}
