package com.origincoding.aquarius.iam.infrastructure.captcha

object RedisPasswordLoginCaptchaKeys {
    fun passwordLoginChallengeKey(challengeId: String): String =
        "aquarius:iam:captcha:password-login:$challengeId"
}
