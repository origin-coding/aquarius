package com.origincoding.aquarius.iam.application.auth

import com.origincoding.aquarius.iam.domain.model.IdentityType
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class DefaultLoginNameNormalizer : LoginNameNormalizer {
    override fun normalize(loginName: String): NormalizedLoginName? {
        val trimmed = loginName.trim()

        if (trimmed.isBlank()) {
            return null
        }

        return if (trimmed.contains(EMAIL_SEPARATOR)) {
            NormalizedLoginName(
                identityType = IdentityType.EMAIL,
                normalizedIdentity = trimmed.lowercase(Locale.ROOT),
            )
        } else {
            NormalizedLoginName(
                identityType = IdentityType.USERNAME,
                normalizedIdentity = trimmed,
            )
        }
    }

    private companion object {
        const val EMAIL_SEPARATOR = "@"
    }
}
