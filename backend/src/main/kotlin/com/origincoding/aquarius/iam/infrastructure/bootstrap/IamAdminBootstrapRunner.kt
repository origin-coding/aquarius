package com.origincoding.aquarius.iam.infrastructure.bootstrap

import com.origincoding.aquarius.iam.application.auth.LoginNameNormalizer
import com.origincoding.aquarius.iam.domain.model.Credential
import com.origincoding.aquarius.iam.domain.model.CredentialType
import com.origincoding.aquarius.iam.domain.model.IamUser
import com.origincoding.aquarius.iam.domain.model.Identity
import com.origincoding.aquarius.iam.domain.model.UserStatus
import com.origincoding.aquarius.iam.domain.repository.CredentialRepository
import com.origincoding.aquarius.iam.domain.repository.IamUserRepository
import com.origincoding.aquarius.iam.domain.repository.IdentityRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@ConditionalOnProperty(
    prefix = "aquarius.iam.bootstrap.admin",
    name = ["enabled"],
    havingValue = "true",
)
class IamAdminBootstrapRunner(
    private val properties: IamAdminBootstrapProperties,
    private val loginNameNormalizer: LoginNameNormalizer,
    private val userRepository: IamUserRepository,
    private val identityRepository: IdentityRepository,
    private val credentialRepository: CredentialRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        val loginName = properties.requiredLoginName()
        val normalizedLoginName = loginNameNormalizer.normalize(loginName)
            ?: throw IllegalStateException("aquarius.iam.bootstrap.admin.login-name must be a valid login name")
        val password = properties.requiredPassword()
        val displayName = properties.requiredDisplayName()

        if (userRepository.count() > 0) {
            logger.info { "IAM admin bootstrap skipped because users already exist" }
            return
        }

        if (identityRepository.existsByIdentityTypeAndNormalizedIdentity(
                normalizedLoginName.identityType,
                normalizedLoginName.normalizedIdentity,
            )
        ) {
            logger.info { "IAM admin bootstrap skipped because the configured identity already exists" }
            return
        }

        val user = userRepository.saveAndFlush(
            IamUser(
                status = UserStatus.ACTIVE,
                name = displayName,
            )
        )
        val identity = identityRepository.saveAndFlush(
            Identity(
                userId = user.id,
                identityType = normalizedLoginName.identityType,
                identity = loginName.trim(),
                normalizedIdentity = normalizedLoginName.normalizedIdentity,
                verifiedAt = Instant.now(),
            )
        )
        credentialRepository.save(
            Credential(
                identityId = identity.id,
                credentialType = CredentialType.PASSWORD,
                secret = passwordEncoder.encode(password)
                    ?: throw IllegalStateException("Password encoder returned null"),
            )
        )

        logger.info { "IAM admin bootstrap created the initial admin user" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
