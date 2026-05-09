package com.origincoding.aquarius.shared.web.error

import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.error.ResultCode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class GlobalResultCodeHttpStatusMapper : ResultCodeHttpStatusMapper {
    override fun mappings(): Map<ResultCode, HttpStatus> = mapOf(
        GlobalResultCode.OK to HttpStatus.OK,

        GlobalResultCode.REQUEST_VALIDATION_FAILED to HttpStatus.BAD_REQUEST,
        GlobalResultCode.REQUEST_MALFORMED to HttpStatus.BAD_REQUEST,
        GlobalResultCode.REQUEST_METHOD_NOT_ALLOWED to HttpStatus.METHOD_NOT_ALLOWED,
        GlobalResultCode.REQUEST_UNSUPPORTED_MEDIA_TYPE to HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        GlobalResultCode.REQUEST_PAYLOAD_TOO_LARGE to HttpStatus.PAYLOAD_TOO_LARGE,

        GlobalResultCode.RESOURCE_NOT_FOUND to HttpStatus.NOT_FOUND,
        GlobalResultCode.RESOURCE_CONFLICT to HttpStatus.CONFLICT,

        GlobalResultCode.OPERATION_NOT_ALLOWED to HttpStatus.CONFLICT,

        GlobalResultCode.UPSTREAM_UNAVAILABLE to HttpStatus.SERVICE_UNAVAILABLE,

        GlobalResultCode.SYSTEM_INTERNAL to HttpStatus.INTERNAL_SERVER_ERROR,
    )
}
