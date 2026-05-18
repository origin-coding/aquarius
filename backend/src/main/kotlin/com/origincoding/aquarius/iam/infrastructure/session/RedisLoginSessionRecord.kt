package com.origincoding.aquarius.iam.infrastructure.session

import java.time.Instant

data class RedisLoginSessionRecord(
    val id: String,
    val userId: String,
    val username: String?,
    val name: String?,
    val accessTokenHash: String,
    val refreshTokenHash: String,
    val createdAt: Instant,
    val refreshExpiresAt: Instant,
)
