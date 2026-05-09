package com.origincoding.aquarius.shared.error

object ValidationIssueCode {
    const val INVALID = "validation.invalid"
    const val REQUIRED = "validation.required"
    const val EMAIL_INVALID = "validation.email.invalid"
    const val SIZE_INVALID = "validation.size.invalid"
    const val MIN = "validation.min"
    const val MAX = "validation.max"
    const val DECIMAL_MIN = "validation.decimal_min"
    const val DECIMAL_MAX = "validation.decimal_max"
    const val PATTERN_INVALID = "validation.pattern.invalid"
    const val POSITIVE = "validation.positive"
    const val POSITIVE_OR_ZERO = "validation.positive_or_zero"
    const val NEGATIVE = "validation.negative"
    const val NEGATIVE_OR_ZERO = "validation.negative_or_zero"
}
