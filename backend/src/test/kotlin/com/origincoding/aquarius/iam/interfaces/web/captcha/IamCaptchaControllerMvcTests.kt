package com.origincoding.aquarius.iam.interfaces.web.captcha

import com.origincoding.aquarius.iam.application.auth.CaptchaDelivery
import com.origincoding.aquarius.iam.application.auth.CaptchaIssuer
import com.origincoding.aquarius.iam.application.auth.CaptchaPurpose
import com.origincoding.aquarius.iam.application.auth.IssueCaptchaCommand
import com.origincoding.aquarius.iam.application.auth.IssuedCaptcha
import com.origincoding.aquarius.iam.application.session.LoginSessionResolver
import com.origincoding.aquarius.iam.interfaces.web.IamResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.web.error.GlobalControllerAdvice
import com.origincoding.aquarius.shared.web.error.GlobalResultCodeHttpStatusMapper
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusRegistry
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [IamCaptchaController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalControllerAdvice::class,
    ResultCodeHttpStatusRegistry::class,
    GlobalResultCodeHttpStatusMapper::class,
    IamResultCodeHttpStatusMapper::class,
)
class IamCaptchaControllerMvcTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var captchaIssuer: CaptchaIssuer

    @MockitoBean
    private lateinit var loginSessionResolver: LoginSessionResolver

    @Test
    fun `issues password login captcha through HTTP contract`() {
        `when`(
            captchaIssuer.issue(
                IssueCaptchaCommand(
                    purpose = CaptchaPurpose.PASSWORD_LOGIN,
                    target = "alice",
                )
            )
        )
            .thenReturn(
                IssuedCaptcha(
                    captchaChallengeId = "challenge-id",
                    delivery = CaptchaDelivery.IMAGE,
                    expiresIn = 300,
                    imageBase64 = "base64-image",
                    imageContentType = "image/png",
                )
            )

        mockMvc.perform(get("/iam/captchas/password-login").param("loginName", "alice"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(GlobalResultCode.OK.code))
            .andExpect(jsonPath("$.data.captchaChallengeId").value("challenge-id"))
            .andExpect(jsonPath("$.data.delivery").value("IMAGE"))
            .andExpect(jsonPath("$.data.expiresIn").value(300))
            .andExpect(jsonPath("$.data.imageBase64").value("base64-image"))
            .andExpect(jsonPath("$.data.imageContentType").value("image/png"))

        verify(captchaIssuer).issue(
            IssueCaptchaCommand(
                purpose = CaptchaPurpose.PASSWORD_LOGIN,
                target = "alice",
            )
        )
    }

    @Test
    fun `rejects password login captcha issue without login name`() {
        mockMvc.perform(get("/iam/captchas/password-login"))
            .andExpect(status().isBadRequest)
    }
}
