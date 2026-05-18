package com.origincoding.aquarius.iam.application.auth

fun interface CaptchaVerifier {
    fun verify(command: VerifyCaptchaCommand)
}

data class VerifyCaptchaCommand(
    val purpose: CaptchaPurpose,
    val code: String,
    val challengeId: String? = null,
    val target: String? = null,
)

enum class CaptchaPurpose {
    PASSWORD_LOGIN
}
