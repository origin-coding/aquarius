package com.origincoding.aquarius.shared.security

data class CurrentUser(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val authorities: Set<String> = emptySet(),
) {
    companion object {
        const val SYSTEM_ID = "SYSTEM"

        fun system(): CurrentUser = CurrentUser(id = SYSTEM_ID)
    }
}
