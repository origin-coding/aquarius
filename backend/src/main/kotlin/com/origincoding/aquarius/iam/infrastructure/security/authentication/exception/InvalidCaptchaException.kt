package com.origincoding.aquarius.iam.infrastructure.security.authentication.exception

import org.springframework.security.core.AuthenticationException

class InvalidCaptchaException(
    message: String = "Invalid captcha"
) : AuthenticationException(message)
