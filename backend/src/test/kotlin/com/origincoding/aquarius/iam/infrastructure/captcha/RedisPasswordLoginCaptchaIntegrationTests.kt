package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.DefaultLoginNameNormalizer
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Duration
import java.time.Instant

@Testcontainers
class RedisPasswordLoginCaptchaIntegrationTests {
    private val properties = PasswordLoginCaptchaProperties(
        ttl = Duration.ofMinutes(5),
        maxAttempts = 3,
        codeLength = 4,
    )
    private val imageGenerator = PasswordLoginCaptchaImageGenerator {
        GeneratedPasswordLoginCaptchaImage(
            code = CAPTCHA_CODE,
            imageBase64 = "base64-image",
            imageContentType = "image/png",
        )
    }
    private val loginNameNormalizer = DefaultLoginNameNormalizer()

    private lateinit var redissonClient: RedissonClient
    private lateinit var captchaStore: RedisPasswordLoginCaptchaStore
    private lateinit var issuer: RedisPasswordLoginCaptchaIssuer
    private lateinit var verifier: RedisPasswordLoginCaptchaVerifier

    @BeforeEach
    fun setUp() {
        redissonClient = Redisson.create(
            Config().apply {
                useSingleServer()
                    .setAddress("redis://${redis.host}:${redis.getMappedPort(REDIS_PORT)}")
            }
        )
        captchaStore =
            RedisPasswordLoginCaptchaStore(redissonClient, jacksonMapperBuilder().findAndAddModules().build())
        issuer = RedisPasswordLoginCaptchaIssuer(
            captchaStore = captchaStore,
            imageGenerator = imageGenerator,
            loginNameNormalizer = loginNameNormalizer,
            properties = properties,
        )
        verifier = RedisPasswordLoginCaptchaVerifier(
            captchaStore = captchaStore,
            loginNameNormalizer = loginNameNormalizer,
            properties = properties,
        )
    }

    @AfterEach
    fun tearDown() {
        if (::redissonClient.isInitialized) {
            redissonClient.keys.flushdb()
            redissonClient.shutdown()
        }
    }

    @Test
    fun `issue stores password login challenge in redis`() {
        val issuedCaptcha = issueCaptcha()
        val record = captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get()

        assertNotNull(record)
        assertEquals(issuedCaptcha.captchaChallengeId, record!!.challengeId)
        assertEquals(CaptchaDelivery.IMAGE, record.delivery)
        assertEquals(0, record.attemptCount)
        assertTrue(record.targetHash!!.isNotBlank())
        assertFalse(record.codeHash.contains(CAPTCHA_CODE))
        assertTrue(PasswordLoginCaptchaCodeHasher().matches(record.challengeId, CAPTCHA_CODE, record.codeHash))
        assertTrue(captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).remainTimeToLive() > 0)
    }

    @Test
    fun `verify accepts correct login name and code then deletes challenge`() {
        val issuedCaptcha = issueCaptcha()

        verifier.verify(
            verifyCommand(
                challengeId = issuedCaptcha.captchaChallengeId,
                loginName = LOGIN_NAME,
                code = CAPTCHA_CODE,
            )
        )

        assertNull(captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get())
    }

    @Test
    fun `verify rejects wrong code and increments attempt count`() {
        val issuedCaptcha = issueCaptcha()

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = issuedCaptcha.captchaChallengeId,
                    loginName = LOGIN_NAME,
                    code = "9999",
                )
            )
        }

        val record = captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get()
        assertNotNull(record)
        assertEquals(1, record!!.attemptCount)
    }

    @Test
    fun `verify deletes challenge after max failed attempts`() {
        val issuedCaptcha = issueCaptcha()

        repeat(properties.maxAttempts) {
            assertThrows<InvalidCaptchaException> {
                verifier.verify(
                    verifyCommand(
                        challengeId = issuedCaptcha.captchaChallengeId,
                        loginName = LOGIN_NAME,
                        code = "9999",
                    )
                )
            }
        }

        assertNull(captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get())
    }

    @Test
    fun `verify rejects mismatched login name`() {
        val issuedCaptcha = issueCaptcha()

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = issuedCaptcha.captchaChallengeId,
                    loginName = "bob",
                    code = CAPTCHA_CODE,
                )
            )
        }

        val record = captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get()
        assertNotNull(record)
        assertEquals(1, record!!.attemptCount)
    }

    @Test
    fun `verify rejects blank challenge id`() {
        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = " ",
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }
    }

    @Test
    fun `verify rejects missing login name and keeps challenge`() {
        val issuedCaptcha = issueCaptcha()

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                VerifyCaptchaCommand(
                    purpose = CaptchaPurpose.PASSWORD_LOGIN,
                    challengeId = issuedCaptcha.captchaChallengeId,
                    target = null,
                    code = CAPTCHA_CODE,
                )
            )
        }

        val record = captchaStore.challengeBucket(issuedCaptcha.captchaChallengeId).get()
        assertNotNull(record)
        assertEquals(0, record!!.attemptCount)
    }

    @Test
    fun `verify rejects missing challenge`() {
        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = "missing-challenge",
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }
    }

    @Test
    fun `verify rejects expired challenge and removes stale record`() {
        val challengeId = "expired-challenge"
        storeChallenge(
            challengeId = challengeId,
            expiresAt = Instant.now().minusSeconds(1),
            ttl = Duration.ofMinutes(1),
        )

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = challengeId,
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }

        assertNull(captchaStore.challengeBucket(challengeId).get())
    }

    @Test
    fun `redis ttl expiry removes challenge before verification`() {
        val challengeId = "ttl-expired-challenge"
        storeChallenge(
            challengeId = challengeId,
            expiresAt = Instant.now().plusSeconds(60),
            ttl = Duration.ofMillis(200),
        )

        waitUntilBucketExpires(challengeId)

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = challengeId,
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }
        assertNull(captchaStore.challengeBucket(challengeId).get())
    }

    @Test
    fun `verify rejects stored challenge id mismatch and removes record`() {
        val challengeId = "challenge-id-mismatch"
        storeChallenge(
            challengeId = challengeId,
            storedChallengeId = "different-challenge-id",
            expiresAt = Instant.now().plusSeconds(60),
            ttl = Duration.ofMinutes(1),
        )

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = challengeId,
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }

        assertNull(captchaStore.challengeBucket(challengeId).get())
    }

    @Test
    fun `verify rejects legacy challenge without target hash and removes record`() {
        val challengeId = "legacy-challenge"
        storeChallenge(
            challengeId = challengeId,
            targetHash = null,
            expiresAt = Instant.now().plusSeconds(60),
            ttl = Duration.ofMinutes(1),
        )

        assertThrows<InvalidCaptchaException> {
            verifier.verify(
                verifyCommand(
                    challengeId = challengeId,
                    loginName = LOGIN_NAME,
                    code = CAPTCHA_CODE,
                )
            )
        }

        assertNull(captchaStore.challengeBucket(challengeId).get())
    }

    private fun issueCaptcha() =
        issuer.issue(
            IssueCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                target = LOGIN_NAME,
            )
        )

    private fun verifyCommand(
        challengeId: String,
        loginName: String,
        code: String,
    ): VerifyCaptchaCommand =
        VerifyCaptchaCommand(
            purpose = CaptchaPurpose.PASSWORD_LOGIN,
            challengeId = challengeId,
            target = loginName,
            code = code,
        )

    private fun storeChallenge(
        challengeId: String,
        ttl: Duration,
        storedChallengeId: String = challengeId,
        targetHash: String? = PasswordLoginCaptchaTargetHasher().hash(loginNameNormalizer.normalize(LOGIN_NAME)!!),
        expiresAt: Instant = Instant.now().plusSeconds(60),
        attemptCount: Int = 0,
    ) {
        captchaStore.challengeBucket(challengeId).set(
            RedisPasswordLoginCaptchaRecord(
                challengeId = storedChallengeId,
                codeHash = PasswordLoginCaptchaCodeHasher().hash(challengeId, CAPTCHA_CODE),
                targetHash = targetHash,
                delivery = CaptchaDelivery.IMAGE,
                expiresAt = expiresAt,
                createdAt = Instant.now().minusSeconds(1),
                attemptCount = attemptCount,
            ),
            ttl,
        )
    }

    @Suppress("SameParameterValue")
    private fun waitUntilBucketExpires(challengeId: String) {
        val deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos()
        while (captchaStore.challengeBucket(challengeId).get() != null && System.nanoTime() < deadline) {
            Thread.sleep(50)
        }
    }

    private class RedisContainer : GenericContainer<RedisContainer>(
        DockerImageName.parse("redis:8-alpine")
    )

    private companion object {
        const val REDIS_PORT = 6379
        const val LOGIN_NAME = "alice"
        const val CAPTCHA_CODE = "1234"

        @Container
        @JvmStatic
        val redis: RedisContainer = RedisContainer()
            .withExposedPorts(REDIS_PORT)
    }
}
