package com.origincoding.aquarius.iam.infrastructure.captcha

import com.wf.captcha.SpecCaptcha
import com.wf.captcha.base.Captcha
import org.springframework.stereotype.Component

fun interface PasswordLoginCaptchaImageGenerator {
    fun generate(codeLength: Int): GeneratedPasswordLoginCaptchaImage
}

data class GeneratedPasswordLoginCaptchaImage(
    val code: String,
    val imageBase64: String,
    val imageContentType: String,
)

@Component
class EasyCaptchaPasswordLoginCaptchaImageGenerator : PasswordLoginCaptchaImageGenerator {
    override fun generate(codeLength: Int): GeneratedPasswordLoginCaptchaImage {
        val captcha = SpecCaptcha(IMAGE_WIDTH, IMAGE_HEIGHT, codeLength)
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER)

        return GeneratedPasswordLoginCaptchaImage(
            code = captcha.text(),
            imageBase64 = captcha.toBase64().substringAfter(BASE64_SEPARATOR),
            imageContentType = IMAGE_CONTENT_TYPE,
        )
    }

    private companion object {
        const val IMAGE_WIDTH = 128
        const val IMAGE_HEIGHT = 42
        const val BASE64_SEPARATOR = "base64,"
        const val IMAGE_CONTENT_TYPE = "image/png"
    }
}
