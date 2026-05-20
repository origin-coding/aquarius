package com.origincoding.aquarius.iam.infrastructure.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TokenHasherTests {
    private val tokenHasher = TokenHasher()

    @Test
    fun `hashes token with sha256 base64 url encoding without padding`() {
        val hash = tokenHasher.hash("access-token")

        assertEquals("Pxa-1wifRlPl7yG_0oJNfzqq7MelmOfonFgOFgapzFI", hash)
        assertFalse(hash.contains("="))
        assertFalse(hash.contains("+"))
        assertFalse(hash.contains("/"))
    }

    @Test
    fun `builds stable redis session keys`() {
        assertEquals(
            "aquarius:iam:session:access:access-hash",
            RedisLoginSessionKeys.accessTokenKey("access-hash"),
        )
        assertEquals(
            "aquarius:iam:session:refresh:refresh-hash",
            RedisLoginSessionKeys.refreshTokenKey("refresh-hash"),
        )
        assertEquals(
            "aquarius:iam:session:record:session-id",
            RedisLoginSessionKeys.sessionKey("session-id"),
        )
        assertEquals(
            "aquarius:iam:session:user-sessions:user-id",
            RedisLoginSessionKeys.userSessionsKey("user-id"),
        )
    }
}
