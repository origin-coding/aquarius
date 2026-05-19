package com.origincoding.aquarius.iam.application.auth

fun interface CaptchaIssuer {
    fun issue(command: IssueCaptchaCommand): IssuedCaptcha
}

data class IssueCaptchaCommand(
    val purpose: CaptchaPurpose,
    val target: String? = null,
)

data class IssuedCaptcha(
    val captchaChallengeId: String,
    val delivery: CaptchaDelivery,
    val expiresIn: Long? = null,
    val imageBase64: String? = null,
    val imageContentType: String? = null,
)

enum class CaptchaDelivery {
    IMAGE,
    EMAIL,
    LOCAL,
}
