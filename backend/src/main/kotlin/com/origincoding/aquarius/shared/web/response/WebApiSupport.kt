package com.origincoding.aquarius.shared.web.response

import com.origincoding.aquarius.shared.error.IssueBody
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Suppress("unused")
interface WebApiSupport {
    fun ok(): JsonResponse<Unit> =
        JsonResponse.ok()

    fun <T : Any> ok(data: T, warnings: List<IssueBody> = emptyList()): JsonResponse<T> =
        JsonResponse.ok(data, warnings)

    fun <T : Any> okNullable(data: T?, warnings: List<IssueBody> = emptyList()): JsonResponse<T?> =
        JsonResponse.okNullable(data, warnings)

    fun okWithWarnings(warnings: List<IssueBody>): JsonResponse<Unit> =
        JsonResponse.okWithWarnings(warnings)

    val currentRequest: HttpServletRequest?
        get() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

    val clientIp: String?
        get() {
            val request = currentRequest ?: return null
            val forwardedFor = request.getHeader("X-Forwarded-For")
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            return forwardedFor ?: request.remoteAddr
        }
}
