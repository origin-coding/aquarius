package com.origincoding.aquarius.platform.persistence.audit

import com.origincoding.aquarius.shared.security.CurrentUserProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditorAware")
class JpaAuditingConfiguration {
    @Bean
    fun currentUserAuditorAware(currentUserProvider: CurrentUserProvider): AuditorAware<String> =
        CurrentUserAuditorAware(currentUserProvider)
}
