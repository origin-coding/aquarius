package com.origincoding.aquarius.shared.error

import com.fasterxml.jackson.annotation.JsonInclude

data class IssueBody(
    val code: String,

    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val field: String? = null,

    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val arguments: List<MessageArgument> = emptyList()
)
