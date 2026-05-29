package com.origincoding.aquarius.iam.infrastructure.captcha

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisPasswordLoginCaptchaKeysTests {
    @Test
    fun `builds stable redis captcha key`() {
        assertEquals(
            "aquarius:iam:captcha:password-login:challenge-id",
            RedisPasswordLoginCaptchaKeys.passwordLoginChallengeKey("challenge-id"),
        )
    }
}
