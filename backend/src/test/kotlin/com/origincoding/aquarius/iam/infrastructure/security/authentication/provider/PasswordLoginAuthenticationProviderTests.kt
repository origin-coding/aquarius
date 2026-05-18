package com.origincoding.aquarius.iam.infrastructure.security.authentication.provider

import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.CaptchaVerifier
import com.origincoding.aquarius.iam.application.auth.DefaultLoginNameNormalizer
import com.origincoding.aquarius.iam.application.auth.VerifyCaptchaCommand
import com.origincoding.aquarius.iam.domain.model.Credential
import com.origincoding.aquarius.iam.domain.model.CredentialType
import com.origincoding.aquarius.iam.domain.model.IamUser
import com.origincoding.aquarius.iam.domain.model.Identity
import com.origincoding.aquarius.iam.domain.model.IdentityType
import com.origincoding.aquarius.iam.domain.model.UserStatus
import com.origincoding.aquarius.iam.domain.repository.CredentialRepository
import com.origincoding.aquarius.iam.domain.repository.IamUserRepository
import com.origincoding.aquarius.iam.domain.repository.IdentityRepository
import com.origincoding.aquarius.iam.infrastructure.security.authentication.exception.InvalidCaptchaException
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.IamAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.authentication.token.PasswordLoginAuthenticationToken
import com.origincoding.aquarius.iam.infrastructure.security.principal.IamAuthenticatedPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import java.util.Optional

class PasswordLoginAuthenticationProviderTests {
    private val captchaVerifier = RecordingCaptchaVerifier()
    private val loginNameNormalizer = DefaultLoginNameNormalizer()
    private val identityRepository = mock(IdentityRepository::class.java)
    private val userRepository = mock(IamUserRepository::class.java)
    private val credentialRepository = mock(CredentialRepository::class.java)
    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
    private val provider = PasswordLoginAuthenticationProvider(
        captchaVerifier = captchaVerifier,
        loginNameNormalizer = loginNameNormalizer,
        identityRepository = identityRepository,
        userRepository = userRepository,
        credentialRepository = credentialRepository,
        passwordEncoder = passwordEncoder,
    )

    @Test
    fun `authenticates password login token`() {
        givenActivePasswordUser()

        val authentication = provider.authenticate(passwordLoginToken())

        assertTrue(authentication.isAuthenticated)
        assertTrue(authentication is IamAuthenticationToken)
        val principal = authentication.principal as IamAuthenticatedPrincipal
        assertEquals("user-id", principal.userId)
        assertEquals("alice", principal.currentUser.username)
        assertEquals(
            VerifyCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                code = "8888",
                challengeId = "local",
                target = "alice",
            ),
            captchaVerifier.receivedCommand,
        )
    }

    @Test
    fun `maps disabled user to disabled exception`() {
        givenActivePasswordUser(userStatus = UserStatus.DISABLED)

        assertThrows<DisabledException> {
            provider.authenticate(passwordLoginToken())
        }
    }

    @Test
    fun `maps invalid credentials to bad credentials exception`() {
        givenActivePasswordUser()

        assertThrows<BadCredentialsException> {
            provider.authenticate(passwordLoginToken(rawPassword = "wrong-password"))
        }
    }

    @Test
    fun `maps unknown identity to bad credentials exception`() {
        `when`(
            identityRepository.findByIdentityTypeAndNormalizedIdentity(IdentityType.USERNAME, "alice")
        ).thenReturn(null)

        assertThrows<BadCredentialsException> {
            provider.authenticate(passwordLoginToken())
        }
    }

    @Test
    fun `maps missing password credential to bad credentials exception`() {
        givenActivePasswordUser(includeCredential = false)

        assertThrows<BadCredentialsException> {
            provider.authenticate(passwordLoginToken())
        }
    }

    @Test
    fun `maps invalid captcha to invalid captcha exception without checking credentials`() {
        captchaVerifier.exception = InvalidCaptchaException()

        assertThrows<InvalidCaptchaException> {
            provider.authenticate(passwordLoginToken())
        }

        verifyNoInteractions(identityRepository, userRepository, credentialRepository)
    }

    private fun passwordLoginToken(
        loginName: String = "alice",
        rawPassword: String = "correct-password",
        captchaChallengeId: String? = "local",
        captchaCode: String = "8888",
    ): PasswordLoginAuthenticationToken =
        PasswordLoginAuthenticationToken.unauthenticated(
            loginName = loginName,
            rawPassword = rawPassword,
            captchaChallengeId = captchaChallengeId,
            captchaCode = captchaCode,
        )

    private fun givenActivePasswordUser(
        userStatus: UserStatus = UserStatus.ACTIVE,
        includeCredential: Boolean = true,
    ) {
        val user = IamUser(status = userStatus, name = "Alice").also {
            it.uuid = "user-id"
        }
        val identity = Identity(
            userId = "user-id",
            identityType = IdentityType.USERNAME,
            identity = "alice",
            normalizedIdentity = "alice",
        ).also {
            it.uuid = "identity-id"
        }
        val credential = Credential(
            userId = "user-id",
            credentialType = CredentialType.PASSWORD,
            secret = passwordEncoder.encode("correct-password")!!,
        ).also {
            it.uuid = "credential-id"
        }

        `when`(
            identityRepository.findByIdentityTypeAndNormalizedIdentity(IdentityType.USERNAME, "alice")
        ).thenReturn(identity)
        `when`(userRepository.findById("user-id")).thenReturn(Optional.of(user))
        `when`(
            credentialRepository.findByUserIdAndCredentialType("user-id", CredentialType.PASSWORD)
        ).thenReturn(if (includeCredential) credential else null)
    }

    private class RecordingCaptchaVerifier : CaptchaVerifier {
        var receivedCommand: VerifyCaptchaCommand? = null
        var exception: RuntimeException? = null

        override fun verify(command: VerifyCaptchaCommand) {
            receivedCommand = command
            exception?.let { throw it }
        }
    }
}
