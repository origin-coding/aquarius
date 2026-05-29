package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.NormalizedLoginName
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class PasswordLoginCaptchaTargetHasher {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun hash(loginName: NormalizedLoginName): String {
        val material = "${loginName.identityType}:${loginName.normalizedIdentity}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(StandardCharsets.UTF_8))

        return encoder.encodeToString(digest)
    }
}
