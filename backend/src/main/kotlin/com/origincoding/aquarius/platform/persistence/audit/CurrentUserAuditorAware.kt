package com.origincoding.aquarius.platform.persistence.audit

import com.origincoding.aquarius.shared.security.CurrentUserProvider
import org.springframework.data.domain.AuditorAware
import java.util.Optional

class CurrentUserAuditorAware(
    private val currentUserProvider: CurrentUserProvider,
) : AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> =
        Optional.of(currentUserProvider.currentUser().id)
}
