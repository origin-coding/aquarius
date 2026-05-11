package com.origincoding.aquarius.platform.security

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserResolver
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SystemCurrentUserResolver : CurrentUserResolver {
    override fun resolveCurrentUser(): CurrentUser =
        CurrentUser.system()
}
