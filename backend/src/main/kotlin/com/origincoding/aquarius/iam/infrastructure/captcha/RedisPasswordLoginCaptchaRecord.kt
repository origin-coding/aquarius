package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import java.time.Instant

data class RedisPasswordLoginCaptchaRecord(
    val challengeId: String,
    val codeHash: String,
    val delivery: CaptchaDelivery,
    val expiresAt: Instant,
    val createdAt: Instant,
    val attemptCount: Int,
)
