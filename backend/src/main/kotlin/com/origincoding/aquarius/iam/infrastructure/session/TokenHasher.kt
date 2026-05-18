package com.origincoding.aquarius.iam.infrastructure.session

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

@Component
class TokenHasher {
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))

        return encoder.encodeToString(digest)
    }
}
