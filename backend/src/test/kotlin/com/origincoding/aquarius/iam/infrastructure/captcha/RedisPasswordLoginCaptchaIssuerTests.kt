package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.DefaultLoginNameNormalizer
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import java.time.Duration

class RedisPasswordLoginCaptchaIssuerTests {
    private val properties = PasswordLoginCaptchaProperties(
        ttl = Duration.ofMinutes(5),
        maxAttempts = 5,
        codeLength = 4,
    )
    private val imageGenerator = PasswordLoginCaptchaImageGenerator {
        GeneratedPasswordLoginCaptchaImage(
            code = "1234",
            imageBase64 = "base64-image",
            imageContentType = "image/png",
        )
    }
    private val captchaStore = mock(RedisPasswordLoginCaptchaStore::class.java)
    private val challengeBucket = mockCaptchaBucket()
    private val issuer = RedisPasswordLoginCaptchaIssuer(
        captchaStore = captchaStore,
        imageGenerator = imageGenerator,
        loginNameNormalizer = DefaultLoginNameNormalizer(),
        properties = properties,
    )

    @Test
    fun `issue stores redis challenge record and returns image captcha contract`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)

        val issuedCaptcha = issuer.issue(
            IssueCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                target = "Alice",
            )
        )

        val recordCaptor = ArgumentCaptor.forClass(RedisPasswordLoginCaptchaRecord::class.java)
        verify(challengeBucket).set(recordCaptor.capture(), eq(properties.ttl))
        val record = recordCaptor.value

        assertEquals(issuedCaptcha.captchaChallengeId, record.challengeId)
        assertEquals(CaptchaDelivery.IMAGE, record.delivery)
        assertEquals(0, record.attemptCount)
        assertTrue(record.targetHash!!.isNotBlank())
        assertTrue(record.expiresAt.isAfter(record.createdAt))
        assertFalse(record.codeHash.contains("1234"))
        assertTrue(PasswordLoginCaptchaCodeHasher().matches(record.challengeId, "1234", record.codeHash))

        assertEquals(CaptchaDelivery.IMAGE, issuedCaptcha.delivery)
        assertEquals(300, issuedCaptcha.expiresIn)
        assertEquals("base64-image", issuedCaptcha.imageBase64)
        assertEquals("image/png", issuedCaptcha.imageContentType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockCaptchaBucket(): RBucket<RedisPasswordLoginCaptchaRecord> =
        mock(RBucket::class.java) as RBucket<RedisPasswordLoginCaptchaRecord>
}
