package com.origincoding.aquarius.iam.infrastructure.security.config

import com.origincoding.aquarius.iam.infrastructure.security.exception.IamAccessDeniedHandler
import com.origincoding.aquarius.iam.infrastructure.security.exception.IamAuthenticationEntryPoint
import com.origincoding.aquarius.iam.infrastructure.security.filter.BearerTokenAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class IamSecurityConfiguration(
    private val iamLoginConfigurer: IamLoginConfigurer,
    private val bearerTokenAuthenticationFilter: BearerTokenAuthenticationFilter,
    private val iamAuthenticationEntryPoint: IamAuthenticationEntryPoint,
    private val iamAccessDeniedHandler: IamAccessDeniedHandler,
    private val environment: Environment,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            authorizeHttpRequests {
                authorize(iamLoginConfigurer.loginRequestMatcher, permitAll)
                authorize(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/iam/auth/sessions/refresh-token"), permitAll)

                // When in local profile, allow all requests to open API.
                if (environment.acceptsProfiles(Profiles.of("local"))) {
                    authorize(openApiRequestMatcher(), permitAll)
                }

                authorize(anyRequest, authenticated)
            }

            exceptionHandling {
                authenticationEntryPoint = iamAuthenticationEntryPoint
                accessDeniedHandler = iamAccessDeniedHandler
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(bearerTokenAuthenticationFilter)
            with(iamLoginConfigurer)
        }

        return http.build()
    }

    private fun openApiRequestMatcher(): RequestMatcher =
        OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/v3/api-docs"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/v3/api-docs/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/swagger-ui/**"),
        )
}
