package com.origincoding.aquarius.iam.infrastructure.captcha

import com.origincoding.aquarius.platform.redis.typedCodec
import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class RedisPasswordLoginCaptchaStore(
    private val redissonClient: RedissonClient,
    objectMapper: ObjectMapper,
) {
    private val captchaRecordCodec = objectMapper.typedCodec<RedisPasswordLoginCaptchaRecord>()

    fun challengeBucket(challengeId: String): RBucket<RedisPasswordLoginCaptchaRecord> =
        redissonClient.getBucket(
            RedisPasswordLoginCaptchaKeys.passwordLoginChallengeKey(challengeId),
            captchaRecordCodec,
        )
}
