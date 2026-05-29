package com.origincoding.aquarius.iam.interfaces.web.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaIssuer
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.IssuedCaptcha
import com.origincoding.aquarius.shared.web.response.JsonResponse
import com.origincoding.aquarius.shared.web.response.WebApiSupport
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/iam/captchas")
class IamCaptchaController(
    private val captchaIssuer: CaptchaIssuer,
) : WebApiSupport {
    @GetMapping("/password-login")
    fun issuePasswordLoginCaptcha(
        @RequestParam loginName: String,
    ): JsonResponse<IssuedCaptcha> = ok(
        captchaIssuer.issue(
            IssueCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                target = loginName.takeIf { it.isNotBlank() }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Login name is required"),
            )
        )
    )
}
