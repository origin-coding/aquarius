package com.origincoding.aquarius.iam.interfaces.web

import com.origincoding.aquarius.iam.application.auth.IamAuthResultCode
import com.origincoding.aquarius.shared.error.ResultCode
import com.origincoding.aquarius.shared.web.error.ResultCodeHttpStatusMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class IamResultCodeHttpStatusMapper : ResultCodeHttpStatusMapper {
    override fun mappings(): Map<ResultCode, HttpStatus> = mapOf(
        IamAuthResultCode.UNAUTHENTICATED to HttpStatus.UNAUTHORIZED,
        IamAuthResultCode.ACCESS_DENIED to HttpStatus.FORBIDDEN,
        IamAuthResultCode.INVALID_CAPTCHA to HttpStatus.UNAUTHORIZED,
        IamAuthResultCode.INVALID_CREDENTIALS to HttpStatus.UNAUTHORIZED,
        IamAuthResultCode.USER_DISABLED to HttpStatus.FORBIDDEN,
        IamAuthResultCode.AUTHENTICATION_FAILED to HttpStatus.UNAUTHORIZED,
    )
}
