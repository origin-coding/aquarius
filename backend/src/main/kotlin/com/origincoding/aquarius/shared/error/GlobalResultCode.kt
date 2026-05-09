package com.origincoding.aquarius.shared.error

@Suppress("unused")
enum class GlobalResultCode(override val code: String) : ResultCode {
    OK("ok"),

    REQUEST_VALIDATION_FAILED("request.validation_failed"),
    REQUEST_MALFORMED("request.malformed"),
    REQUEST_METHOD_NOT_ALLOWED("request.method_not_allowed"),
    REQUEST_UNSUPPORTED_MEDIA_TYPE("request.unsupported_media_type"),
    REQUEST_PAYLOAD_TOO_LARGE("request.payload_too_large"),

    RESOURCE_NOT_FOUND("resource.not_found"),
    RESOURCE_CONFLICT("resource.conflict"),

    OPERATION_NOT_ALLOWED("operation.not_allowed"),

    UPSTREAM_UNAVAILABLE("upstream.unavailable"),

    SYSTEM_INTERNAL("system.internal")
}
