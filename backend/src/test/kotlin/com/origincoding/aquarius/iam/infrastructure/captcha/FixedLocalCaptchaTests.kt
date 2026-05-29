package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FixedLocalCaptchaTests {
    @Test
    fun `fixed local issuer keeps local challenge contract`() {
        val issuedCaptcha = FixedLocalCaptchaIssuer().issue(IssueCaptchaCommand(CaptchaPurpose.PASSWORD_LOGIN))

        assertEquals("local", issuedCaptcha.captchaChallengeId)
        assertEquals(CaptchaDelivery.LOCAL, issuedCaptcha.delivery)
        assertNull(issuedCaptcha.expiresIn)
        assertNull(issuedCaptcha.imageBase64)
        assertNull(issuedCaptcha.imageContentType)
    }

    @Test
    fun `fixed local verifier accepts configured local captcha`() {
        FixedLocalCaptchaVerifier().verify(
            VerifyCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                code = "8888",
                challengeId = "local",
            )
        )
    }

    @Test
    fun `fixed local verifier rejects invalid captcha`() {
        assertThrows<InvalidCaptchaException> {
            FixedLocalCaptchaVerifier().verify(
                VerifyCaptchaCommand(
                    purpose = CaptchaPurpose.PASSWORD_LOGIN,
                    code = "1234",
                    challengeId = "local",
                )
            )
        }
    }
}
