package com.origincoding.aquarius.iam.infrastructure.security.authentication.converter

import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.util.matcher.RequestMatcher

interface IamAuthenticationConverter : AuthenticationConverter {
    val requestMatcher: RequestMatcher
}
