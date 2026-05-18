package com.origincoding.aquarius.iam.infrastructure.session

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class SecureOpaqueTokenGenerator {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(byteLength: Int = DEFAULT_BYTE_LENGTH): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    private companion object {
        const val DEFAULT_BYTE_LENGTH = 32
    }
}
