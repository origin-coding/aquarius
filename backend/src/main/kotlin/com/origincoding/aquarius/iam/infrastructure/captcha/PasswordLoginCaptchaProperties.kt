package com.origincoding.aquarius.iam.infrastructure.captcha

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "aquarius.iam.captcha.password-login")
class PasswordLoginCaptchaProperties(
    val ttl: Duration = Duration.ofMinutes(5),
    val maxAttempts: Int = 5,
    val codeLength: Int = 4,
) {
    init {
        require(!ttl.isNegative && !ttl.isZero) { "Password login captcha TTL must be positive" }
        require(maxAttempts > 0) { "Password login captcha max attempts must be positive" }
        require(codeLength > 0) { "Password login captcha code length must be positive" }
    }
}
