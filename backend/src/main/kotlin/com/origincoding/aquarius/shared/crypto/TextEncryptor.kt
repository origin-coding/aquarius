package com.origincoding.aquarius.shared.crypto

interface TextEncryptor {
    fun encrypt(content: String): String?

    fun decrypt(content: String): String?
}
