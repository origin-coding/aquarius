package com.origincoding.aquarius.iam.infrastructure.captcha

import org.springframework.beans.factory.ObjectProvider
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/*
 * This sender is kept as mail infrastructure only. Do not wire it into password-login
 * captcha issuing until email captcha can be bound to a registered account email
 * instead of an arbitrary user-supplied address.
 */
@Component
class PasswordLoginCaptchaMailSender(
    private val mailSender: ObjectProvider<JavaMailSender>,
    private val mailProperties: PasswordLoginCaptchaMailProperties,
    private val captchaProperties: PasswordLoginCaptchaProperties,
) {
    fun send(command: SendPasswordLoginCaptchaMailCommand) {
        val message = SimpleMailMessage()
        message.from = mailProperties.from
        message.setTo(command.email)
        message.subject = mailProperties.subject
        message.text = buildText(command.code)

        mailSender.getObject().send(message)
    }

    private fun buildText(code: String): String =
        """
        Your Aquarius login verification code is $code.

        This code expires in ${captchaProperties.ttl.toMinutes()} minutes. If you did not request this code, ignore this email.
        """.trimIndent()
}

data class SendPasswordLoginCaptchaMailCommand(
    val email: String,
    val code: String,
)
