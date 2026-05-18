package com.origincoding.aquarius.iam.infrastructure.security

import com.origincoding.aquarius.iam.application.auth.LoginNameNormalizer
import com.origincoding.aquarius.iam.domain.model.CredentialType
import com.origincoding.aquarius.iam.domain.model.UserStatus
import com.origincoding.aquarius.iam.domain.repository.CredentialRepository
import com.origincoding.aquarius.iam.domain.repository.IamUserRepository
import com.origincoding.aquarius.iam.domain.repository.IdentityRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PasswordLoginAuthenticationProvider(
    private val loginNameNormalizer: LoginNameNormalizer,
    private val identityRepository: IdentityRepository,
    private val userRepository: IamUserRepository,
    private val credentialRepository: CredentialRepository,
    private val passwordEncoder: PasswordEncoder,
) : AuthenticationProvider {
    @Transactional(readOnly = true)
    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as PasswordLoginAuthenticationToken
        val password = token.credentials?.toString()
            ?: throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)

        val normalizedLoginName = loginNameNormalizer.normalize(token.loginName)
            ?: throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)

        val identity = identityRepository.findByIdentityTypeAndNormalizedIdentity(
            normalizedLoginName.identityType,
            normalizedLoginName.normalizedIdentity,
        ) ?: throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)

        val user = userRepository.findById(identity.userId).orElse(null)
            ?: throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)

        if (user.status != UserStatus.ACTIVE) {
            throw DisabledException("User is disabled")
        }

        val credential = credentialRepository.findByUserIdAndCredentialType(
            user.id,
            CredentialType.PASSWORD,
        ) ?: throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)

        if (!passwordEncoder.matches(password, credential.secret)) {
            throw BadCredentialsException(INVALID_CREDENTIALS_MESSAGE)
        }

        return PasswordLoginAuthenticationToken.authenticated(
            IamAuthenticatedPrincipal(
                userId = user.id,
                identityId = identity.id,
                identityType = identity.identityType,
                identity = identity.identity,
                displayName = user.name,
            )
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        PasswordLoginAuthenticationToken::class.java.isAssignableFrom(authentication)

    private companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid login name or password"
    }
}
