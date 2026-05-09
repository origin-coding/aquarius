package com.origincoding.aquarius.shared.error

data class MessageArgument(val name: String, val value: String)

fun arg(name: String, value: Any) = MessageArgument(name, value.toString())

