package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import java.time.Duration
import java.time.Instant

class RedisPasswordLoginCaptchaVerifierTests {
    private val properties = PasswordLoginCaptchaProperties(
        ttl = Duration.ofMinutes(5),
        maxAttempts = 3,
        codeLength = 4,
    )
    private val captchaStore = mock(RedisPasswordLoginCaptchaStore::class.java)
    private val challengeBucket = mockCaptchaBucket()
    private val verifier = RedisPasswordLoginCaptchaVerifier(
        captchaStore = captchaStore,
        properties = properties,
    )

    @Test
    fun `verify accepts correct code and deletes redis challenge`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)
        `when`(challengeBucket.get()).thenReturn(record(code = "1234"))

        verifier.verify(command(code = "1234"))

        verify(challengeBucket).delete()
        verify(challengeBucket, never()).set(any(), any(Duration::class.java))
    }

    @Test
    fun `verify rejects wrong code and increments attempt count`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)
        `when`(challengeBucket.get()).thenReturn(record(code = "1234", attemptCount = 0))

        assertThrows<InvalidCaptchaException> {
            verifier.verify(command(code = "9999"))
        }

        val recordCaptor = ArgumentCaptor.forClass(RedisPasswordLoginCaptchaRecord::class.java)
        verify(challengeBucket).set(recordCaptor.capture(), any(Duration::class.java))
        assertEquals(1, recordCaptor.value.attemptCount)
        verify(challengeBucket, never()).delete()
    }

    @Test
    fun `verify rejects wrong code and deletes challenge after max attempts`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)
        `when`(challengeBucket.get()).thenReturn(record(code = "1234", attemptCount = 2))

        assertThrows<InvalidCaptchaException> {
            verifier.verify(command(code = "9999"))
        }

        verify(challengeBucket).delete()
        verify(challengeBucket, never()).set(any(), any(Duration::class.java))
    }

    @Test
    fun `verify rejects missing challenge`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)
        `when`(challengeBucket.get()).thenReturn(null)

        assertThrows<InvalidCaptchaException> {
            verifier.verify(command(code = "1234"))
        }

        verify(challengeBucket, never()).delete()
    }

    @Test
    fun `verify rejects expired challenge and deletes redis record`() {
        `when`(captchaStore.challengeBucket(anyString())).thenReturn(challengeBucket)
        `when`(challengeBucket.get()).thenReturn(
            record(code = "1234", expiresAt = Instant.now().minusSeconds(1))
        )

        assertThrows<InvalidCaptchaException> {
            verifier.verify(command(code = "1234"))
        }

        verify(challengeBucket).delete()
    }

    private fun command(code: String): VerifyCaptchaCommand =
        VerifyCaptchaCommand(
            purpose = CaptchaPurpose.PASSWORD_LOGIN,
            code = code,
            challengeId = CHALLENGE_ID,
            target = "alice",
        )

    private fun record(
        code: String,
        attemptCount: Int = 0,
        expiresAt: Instant = Instant.now().plusSeconds(300),
    ): RedisPasswordLoginCaptchaRecord {
        val codeHasher = PasswordLoginCaptchaCodeHasher()
        return RedisPasswordLoginCaptchaRecord(
            challengeId = CHALLENGE_ID,
            codeHash = codeHasher.hash(CHALLENGE_ID, code),
            delivery = CaptchaDelivery.IMAGE,
            expiresAt = expiresAt,
            createdAt = Instant.now(),
            attemptCount = attemptCount,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockCaptchaBucket(): RBucket<RedisPasswordLoginCaptchaRecord> =
        mock(RBucket::class.java) as RBucket<RedisPasswordLoginCaptchaRecord>

    private companion object {
        const val CHALLENGE_ID = "challenge-id"
    }
}
