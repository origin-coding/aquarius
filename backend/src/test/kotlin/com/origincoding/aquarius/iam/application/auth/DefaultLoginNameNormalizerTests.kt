package com.origincoding.aquarius.iam.application.auth

import com.origincoding.aquarius.iam.domain.model.IdentityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DefaultLoginNameNormalizerTests {
    private val normalizer = DefaultLoginNameNormalizer()

    @Test
    fun `normalizes username login name`() {
        val result = normalizer.normalize(" alice ")

        assertEquals(NormalizedLoginName(IdentityType.USERNAME, "alice"), result)
    }

    @Test
    fun `normalizes email login name`() {
        val result = normalizer.normalize(" Alice@Example.COM ")

        assertEquals(NormalizedLoginName(IdentityType.EMAIL, "alice@example.com"), result)
    }

    @Test
    fun `rejects blank login name`() {
        assertNull(normalizer.normalize("   "))
    }
}
