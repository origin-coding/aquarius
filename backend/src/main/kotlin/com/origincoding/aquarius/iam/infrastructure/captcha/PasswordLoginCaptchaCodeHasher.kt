package com.origincoding.aquarius.iam.infrastructure.captcha

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class PasswordLoginCaptchaCodeHasher {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun hash(challengeId: String, code: String): String {
        val material = "$challengeId:${normalize(code)}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(StandardCharsets.UTF_8))

        return encoder.encodeToString(digest)
    }

    fun matches(challengeId: String, code: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(challengeId, code).toByteArray(StandardCharsets.UTF_8),
            expectedHash.toByteArray(StandardCharsets.UTF_8),
        )

    private fun normalize(code: String): String =
        code.trim().uppercase()
}
