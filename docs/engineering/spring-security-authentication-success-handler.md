# Spring Security Authentication Success Handler

This document records an Aquarius-specific Spring Security 7 integration pitfall around
`AuthenticationSuccessHandler`.

## Scope

Use this guideline when adding or changing custom authentication filters, login endpoints,
or `AuthenticationSuccessHandler` implementations.

Aquarius uses Spring Security's `AuthenticationFilter` for IAM login endpoints such as:

```text
POST /iam/auth/sessions/password
```

Those login endpoints are handled by the security filter chain, not by a normal MVC
controller method.

## Problem

In Spring Security 7, `AuthenticationSuccessHandler` has a four-argument callback:

```kotlin
onAuthenticationSuccess(request, response, chain, authentication)
```

The default implementation calls the older three-argument callback and then continues the
filter chain:

```text
onAuthenticationSuccess(request, response, authentication)
chain.doFilter(request, response)
```

If a custom success handler only implements the three-argument callback, it may write a
successful JSON login response and then still allow the request to continue through the
remaining filter chain.

For login URLs that do not have a backing MVC controller, the request can then fall through
to MVC/static-resource handling and produce a `404` after the success response path has
already run.

Observed symptom:

```text
POST /iam/auth/sessions/password
-> authentication succeeds
-> success handler writes response
-> filter chain continues
-> no MVC handler exists for the login URL
-> final response may become 404
```

This is easy to misdiagnose because authentication, session issuing, and response writing
can all appear to work during debugging.

## Required Pattern

Custom success handlers that produce the final HTTP response must override the four-argument
callback and must not continue the filter chain:

```kotlin
override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    chain: FilterChain,
    authentication: Authentication,
) {
    onAuthenticationSuccess(request, response, authentication)
}
```

The three-argument callback can contain the shared response-writing logic:

```kotlin
override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
) {
    response.status = HttpStatus.OK.value()
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    jsonMapper.writeValue(response.outputStream, JsonResponse.ok(data))
}
```

Do not call `chain.doFilter(...)` after writing the final login response.

## When Continuing The Chain Is Acceptable

Continuing the filter chain is only appropriate when the success handler is intentionally
not the final response owner.

Examples:

- Authentication enriches the security context and a later MVC controller should handle the request.
- The handler only records metrics or audit information and does not commit the response.

For Aquarius IAM login endpoints, the success handler is the response owner, so it should
terminate the chain after writing the JSON response.

## Testing Guidance

Add a MockMvc test that exercises the authentication filter, not only the provider.

The test should verify:

- `POST /iam/auth/sessions/password` returns `200`.
- The response body contains `code = ok`.
- The response body contains issued session fields.
- The request does not fall through to `404`.

This kind of test catches filter-chain integration bugs that provider-only unit tests cannot
detect.

## Aquarius Reference

Current implementation:

```text
backend/src/main/kotlin/com/origincoding/aquarius/iam/infrastructure/security/authentication/handler/IamAuthenticationSuccessHandler.kt
```

Current regression coverage:

```text
backend/src/test/kotlin/com/origincoding/aquarius/iam/infrastructure/security/authentication/PasswordLoginAuthenticationFilterMvcTests.kt
```
