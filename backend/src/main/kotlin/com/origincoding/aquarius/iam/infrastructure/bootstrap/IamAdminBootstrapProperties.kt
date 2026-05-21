package com.origincoding.aquarius.iam.infrastructure.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aquarius.iam.bootstrap.admin")
class IamAdminBootstrapProperties(
    val enabled: Boolean = false,
    val loginName: String? = null,
    val password: String? = null,
    val displayName: String = DEFAULT_DISPLAY_NAME,
) {
    fun requiredLoginName(): String =
        loginName?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("aquarius.iam.bootstrap.admin.login-name must be configured when admin bootstrap is enabled")

    fun requiredPassword(): String =
        password?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("aquarius.iam.bootstrap.admin.password must be configured when admin bootstrap is enabled")

    fun requiredDisplayName(): String =
        displayName.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("aquarius.iam.bootstrap.admin.display-name must not be blank")

    private companion object {
        const val DEFAULT_DISPLAY_NAME = "System Admin"
    }
}
