package com.origincoding.aquarius.iam.infrastructure.captcha

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.time.Duration

class PasswordLoginCaptchaMailSenderTests {
    private val javaMailSender = mock(JavaMailSender::class.java)
    private val javaMailSenderProvider = mockJavaMailSenderProvider()
    private val mailSender = PasswordLoginCaptchaMailSender(
        mailSender = javaMailSenderProvider,
        mailProperties = PasswordLoginCaptchaMailProperties(
            from = "security@aquarius.local",
            subject = "Login code",
        ),
        captchaProperties = PasswordLoginCaptchaProperties(
            ttl = Duration.ofMinutes(5),
            maxAttempts = 5,
            codeLength = 6,
        ),
    )

    @Test
    fun `sends password login captcha email`() {
        `when`(javaMailSenderProvider.getObject()).thenReturn(javaMailSender)

        mailSender.send(
            SendPasswordLoginCaptchaMailCommand(
                email = "alice@example.com",
                code = "123456",
            )
        )

        val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage::class.java)
        verify(javaMailSender).send(messageCaptor.capture())
        val message = messageCaptor.value

        assertEquals("security@aquarius.local", message.from)
        assertArrayEquals(arrayOf("alice@example.com"), message.to)
        assertEquals("Login code", message.subject)
        assertTrue(message.text!!.contains("123456"))
        assertTrue(message.text!!.contains("5 minutes"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockJavaMailSenderProvider(): ObjectProvider<JavaMailSender> =
        mock(ObjectProvider::class.java) as ObjectProvider<JavaMailSender>
}
