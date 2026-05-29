package com.origincoding.aquarius.iam.infrastructure.captcha

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aquarius.iam.captcha.password-login.mail")
class PasswordLoginCaptchaMailProperties(
    val from: String = "no-reply@aquarius.local",
    val subject: String = "Aquarius login verification code",
)
