package com.origincoding.aquarius.shared.web.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.error.IssueBody
import com.origincoding.aquarius.shared.error.MessageArgument
import com.origincoding.aquarius.shared.error.ResultCode
import org.slf4j.MDC
import java.time.Instant

@Suppress("unused")
sealed interface JsonResponse<out T> {
    val code: String
    val arguments: List<MessageArgument>
    val issues: List<IssueBody>
    val warnings: List<IssueBody>
    val timestamp: Instant
    val requestId: String

    @ConsistentCopyVisibility
    data class WithoutData internal constructor(
        override val code: String,

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val arguments: List<MessageArgument> = emptyList(),

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val issues: List<IssueBody> = emptyList(),

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val warnings: List<IssueBody> = emptyList(),

        override val timestamp: Instant = Instant.now(),
        override val requestId: String = currentRequestId()
    ) : JsonResponse<Nothing>

    @ConsistentCopyVisibility
    data class WithData<out T> internal constructor(
        override val code: String,

        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        val data: T?,

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val arguments: List<MessageArgument> = emptyList(),

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val issues: List<IssueBody> = emptyList(),

        @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
        override val warnings: List<IssueBody> = emptyList(),

        override val timestamp: Instant = Instant.now(),
        override val requestId: String = currentRequestId()
    ) : JsonResponse<T>

    companion object {
        fun ok(): JsonResponse<Unit> = WithoutData(code = GlobalResultCode.OK.code)

        fun okWithWarnings(warnings: List<IssueBody> = emptyList()): JsonResponse<Unit> =
            WithoutData(
                code = GlobalResultCode.OK.code,
                warnings = warnings
            )

        fun <T : Any> ok(data: T, warnings: List<IssueBody> = emptyList()): JsonResponse<T> =
            WithData(
                code = GlobalResultCode.OK.code,
                data = data,
                warnings = warnings
            )

        fun <T : Any> okNullable(data: T?, warnings: List<IssueBody> = emptyList()): JsonResponse<T?> =
            WithData(
                code = GlobalResultCode.OK.code,
                data = data,
                warnings = warnings
            )

        fun error(
            resultCode: ResultCode,
            arguments: List<MessageArgument> = emptyList(),
            issues: List<IssueBody> = emptyList()
        ): JsonResponse<Unit> =
            WithoutData(
                code = resultCode.code,
                arguments = arguments,
                issues = issues
            )

        private fun currentRequestId(): String = MDC.get("traceId") ?: "unknown"
    }
}
