package com.origincoding.aquarius.platform.security

import com.origincoding.aquarius.shared.security.CurrentUser
import com.origincoding.aquarius.shared.security.CurrentUserContext
import com.origincoding.aquarius.shared.security.CurrentUserResolver
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
class ContextCurrentUserResolver : CurrentUserResolver {
    override fun resolveCurrentUser(): CurrentUser? =
        CurrentUserContext.currentUser()
}
