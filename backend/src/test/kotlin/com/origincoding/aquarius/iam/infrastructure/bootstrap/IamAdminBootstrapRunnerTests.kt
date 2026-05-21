package com.origincoding.aquarius.iam.infrastructure.bootstrap

import com.origincoding.aquarius.iam.application.auth.DefaultLoginNameNormalizer
import com.origincoding.aquarius.iam.domain.model.Credential
import com.origincoding.aquarius.iam.domain.model.CredentialType
import com.origincoding.aquarius.iam.domain.model.IamUser
import com.origincoding.aquarius.iam.domain.model.Identity
import com.origincoding.aquarius.iam.domain.model.IdentityType
import com.origincoding.aquarius.iam.domain.repository.CredentialRepository
import com.origincoding.aquarius.iam.domain.repository.IamUserRepository
import com.origincoding.aquarius.iam.domain.repository.IdentityRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.boot.ApplicationArguments
import org.springframework.security.crypto.factory.PasswordEncoderFactories

class IamAdminBootstrapRunnerTests {
    private val loginNameNormalizer = DefaultLoginNameNormalizer()
    private val userRepository = mock(IamUserRepository::class.java)
    private val identityRepository = mock(IdentityRepository::class.java)
    private val credentialRepository = mock(CredentialRepository::class.java)
    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Test
    fun `creates initial admin when user table is empty`() {
        givenEmptyIam()
        val runner = runner(
            IamAdminBootstrapProperties(
                enabled = true,
                loginName = "Admin@Example.COM",
                password = "correct-password",
                displayName = "Root Admin",
            )
        )

        runner.run(mock(ApplicationArguments::class.java))

        val userCaptor = ArgumentCaptor.forClass(IamUser::class.java)
        verify(userRepository).saveAndFlush(userCaptor.capture())
        assertEquals("Root Admin", userCaptor.value.name)

        val identityCaptor = ArgumentCaptor.forClass(Identity::class.java)
        verify(identityRepository).saveAndFlush(identityCaptor.capture())
        assertEquals("user-id", identityCaptor.value.userId)
        assertEquals(IdentityType.EMAIL, identityCaptor.value.identityType)
        assertEquals("Admin@Example.COM", identityCaptor.value.identity)
        assertEquals("admin@example.com", identityCaptor.value.normalizedIdentity)
        assertTrue(identityCaptor.value.verifiedAt != null)

        val credentialCaptor = ArgumentCaptor.forClass(Credential::class.java)
        verify(credentialRepository).save(credentialCaptor.capture())
        assertEquals("identity-id", credentialCaptor.value.identityId)
        assertEquals(CredentialType.PASSWORD, credentialCaptor.value.credentialType)
        assertTrue(passwordEncoder.matches("correct-password", credentialCaptor.value.secret))
    }

    @Test
    fun `skips bootstrap when users already exist`() {
        `when`(userRepository.count()).thenReturn(1)
        val runner = runner()

        runner.run(mock(ApplicationArguments::class.java))

        verify(userRepository, never()).saveAndFlush(any(IamUser::class.java))
        verifyNoInteractions(identityRepository, credentialRepository)
    }

    @Test
    fun `rejects enabled bootstrap without password`() {
        val runner = runner(IamAdminBootstrapProperties(enabled = true, loginName = "admin"))

        assertThrows<IllegalStateException> {
            runner.run(mock(ApplicationArguments::class.java))
        }

        verifyNoInteractions(userRepository, identityRepository, credentialRepository)
    }

    private fun givenEmptyIam() {
        `when`(userRepository.count()).thenReturn(0)
        `when`(
            identityRepository.existsByIdentityTypeAndNormalizedIdentity(
                IdentityType.EMAIL,
                "admin@example.com",
            )
        ).thenReturn(false)
        doAnswer { invocation ->
            (invocation.arguments[0] as IamUser).also {
                it.uuid = "user-id"
            }
        }.`when`(userRepository).saveAndFlush(any(IamUser::class.java))
        doAnswer { invocation ->
            (invocation.arguments[0] as Identity).also {
                it.uuid = "identity-id"
            }
        }.`when`(identityRepository).saveAndFlush(any(Identity::class.java))
    }

    private fun runner(
        properties: IamAdminBootstrapProperties = IamAdminBootstrapProperties(
            enabled = true,
            loginName = "admin",
            password = "correct-password",
        )
    ): IamAdminBootstrapRunner =
        IamAdminBootstrapRunner(
            properties = properties,
            loginNameNormalizer = loginNameNormalizer,
            userRepository = userRepository,
            identityRepository = identityRepository,
            credentialRepository = credentialRepository,
            passwordEncoder = passwordEncoder,
        )

    private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
}
