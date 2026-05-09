package com.origincoding.aquarius.shared.error

open class BusinessException(
    open val resultCode: ResultCode,
    open val arguments: List<MessageArgument> = emptyList(),
    open val issues: List<IssueBody> = emptyList(),
    cause: Throwable? = null,
) : RuntimeException(resultCode.code, cause)
