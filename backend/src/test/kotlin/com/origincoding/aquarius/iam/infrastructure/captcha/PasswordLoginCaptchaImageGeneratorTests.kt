package com.origincoding.aquarius.iam.infrastructure.captcha

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

class PasswordLoginCaptchaImageGeneratorTests {
    @Test
    fun `generates png base64 image with requested code length`() {
        val captcha = EasyCaptchaPasswordLoginCaptchaImageGenerator().generate(codeLength = 4)

        assertEquals(4, captcha.code.length)
        assertEquals("image/png", captcha.imageContentType)

        val imageBytes = Base64.getDecoder().decode(captcha.imageBase64)
        val image = ImageIO.read(ByteArrayInputStream(imageBytes))
        assertNotNull(image)
        assertEquals(128, image.width)
        assertEquals(42, image.height)
    }
}
