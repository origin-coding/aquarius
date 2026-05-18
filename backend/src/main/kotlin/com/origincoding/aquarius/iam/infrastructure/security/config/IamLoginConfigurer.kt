package com.origincoding.aquarius.iam.infrastructure.security.config

import com.origincoding.aquarius.iam.infrastructure.security.authentication.converter.IamAuthenticationConverter
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationFailureHandler
import com.origincoding.aquarius.iam.infrastructure.security.authentication.handler.IamAuthenticationSuccessHandler
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.web.authentication.AuthenticationFilter
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.stereotype.Component

@Component
class IamLoginConfigurer(
    private val converters: List<IamAuthenticationConverter>,
    private val providers: List<AuthenticationProvider>,
    private val iamAuthenticationSuccessHandler: IamAuthenticationSuccessHandler,
    private val iamAuthenticationFailureHandler: IamAuthenticationFailureHandler,
) : AbstractHttpConfigurer<IamLoginConfigurer, HttpSecurity>() {
    val loginRequestMatcher: RequestMatcher =
        OrRequestMatcher(converters.map { it.requestMatcher })

    override fun configure(builder: HttpSecurity) {
        val authenticationManager = ProviderManager(providers)
        val authenticationConverter = DelegatingAuthenticationConverter(converters)

        val authenticationFilter = AuthenticationFilter(authenticationManager, authenticationConverter).apply {
            requestMatcher = loginRequestMatcher
            successHandler = iamAuthenticationSuccessHandler
            failureHandler = iamAuthenticationFailureHandler
        }

        builder.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
    }
}
