package com.origincoding.aquarius.iam.application.auth

import com.origincoding.aquarius.shared.error.ResultCode

enum class IamAuthResultCode(override val code: String) : ResultCode {
    UNAUTHENTICATED("iam.auth.unauthenticated"),
    ACCESS_DENIED("iam.auth.access_denied"),
    INVALID_CAPTCHA("iam.auth.invalid_captcha"),
    INVALID_CREDENTIALS("iam.auth.invalid_credentials"),
    USER_DISABLED("iam.auth.user_disabled"),
    AUTHENTICATION_FAILED("iam.auth.authentication_failed"),
}
