# IAM Architecture and Password Captcha Login Design

Status: temporary draft

## Context

The current IAM module should be organized around business boundaries first. Spring Security should be treated as infrastructure used to adapt HTTP authentication into IAM application use cases, not as the core structure of the IAM module.

The login flow discussed here is password login with captcha verification. It is not a separate captcha-login flow. Captcha verification is a required pre-check in the password login use case.

## Recommended IAM Package Structure

```text
iam
  domain
    model
    repository

  application
    auth
    user
    credential
    session
    command
    result

  interfaces
    web

  infrastructure
    security
      config
      authentication
        provider
        token
        handler
        converter
      filter
      principal
    captcha
    token
    property
```

## Package Responsibilities

`iam.domain.model` contains IAM domain entities and enums, such as `IamUser`, `Identity`, `Credential`, `UserStatus`, `IdentityType`, and `CredentialType`.

`iam.domain.repository` contains repository contracts for IAM domain objects. For the current project stage, these repositories can continue to extend Spring Data JPA repositories directly. A stricter persistence adapter split can be introduced later if the module grows.

`iam.application` contains use-case orchestration. Login, logout, captcha verification abstraction, login name normalization, password reset, credential changes, and session lifecycle logic should live here.

`iam.interfaces.web` contains HTTP controllers and request/response DTOs. Controllers should translate HTTP input into application commands and should not contain core authentication rules.

`iam.infrastructure.security` contains Spring Security integration: filter chain configuration, authentication providers, authentication tokens, converters, handlers, filters, and principal adapters.

`iam.infrastructure.captcha` contains captcha implementations, such as the local fixed-code verifier and future Redis/image captcha implementations.

`iam.infrastructure.token` contains token issuing, parsing, persistence, or revocation implementations.

`iam.infrastructure.property` should only contain infrastructure configuration properties when needed. There is no need to keep a top-level `property` package for a single setting.

## Migration Mapping From Previous Project

```text
entity              -> iam.domain.model
repository          -> iam.domain.repository
controller          -> iam.interfaces.web
property            -> iam.infrastructure.<area>.*Properties
config              -> iam.infrastructure.security.config
security.converter  -> iam.infrastructure.security.authentication.converter
security.provider   -> iam.infrastructure.security.authentication.provider
security.filter     -> iam.infrastructure.security.filter
security.handler    -> iam.infrastructure.security.authentication.handler
security.dto        -> iam.interfaces.web.dto or shared.error
security.service    -> iam.application.auth or iam.application.session
security.token      -> iam.infrastructure.security.authentication.token or iam.infrastructure.token
```

## Design Principles

The IAM domain should not be structured around Spring Security package names. Spring Security classes are adapters around IAM use cases.

Application services should own login orchestration. Authentication providers can delegate to application services or remain thin adapters.

Other modules should not depend on IAM entities, IAM authentication tokens, or IAM principals directly. Cross-module access should go through stable shared abstractions such as `shared.security.CurrentUserProvider` and `shared.security.CurrentUser`.

Avoid introducing resolver/provider/composite chains unless there are multiple real runtime sources to resolve from. `CurrentUser` needs this kind of structure because it can come from the security context, system context, or manually bound context. Captcha verification does not currently need that complexity.

## Password Login With Captcha

The intended flow is:

```text
1. Receive loginName, password, captchaChallengeId, and captchaCode.
2. Verify captcha.
3. Authenticate loginName and password.
4. Issue token or create session.
5. Return login result.
```

Suggested application-level types for the direct Spring Security login design:

```text
iam.application.auth
  CaptchaIssuer
  CaptchaVerifier
  LoginNameNormalizer
```

The password-login request itself should be handled by Spring Security:

```text
AuthenticationFilter
PasswordLoginAuthenticationConverter
PasswordLoginAuthenticationToken
PasswordLoginAuthenticationProvider
SuccessHandler / FailureHandler
```

Captcha verification should be a small application abstraction:

```kotlin
interface CaptchaVerifier {
    fun verify(command: VerifyCaptchaCommand)
}

data class VerifyCaptchaCommand(
    val purpose: CaptchaPurpose,
    val code: String,
    val challengeId: String? = null,
    val target: String? = null,
)

enum class CaptchaPurpose {
    PASSWORD_LOGIN,
}
```

`VerifyCaptchaCommand` should not permanently assume every captcha has a required `key`. Different captcha types use different locator information:

```text
Image captcha:
  challengeId + code
  The challengeId identifies the anonymous generated image challenge.

SMS captcha:
  purpose + target + code
  The target is usually a phone number.

Email captcha:
  purpose + target + code
  The target is usually an email address.

High-risk operation captcha:
  purpose + challengeId or target + code
  The locator can be an operation challenge id, user id, or action id.
```

Field meaning:

```text
purpose:
  why this captcha is being verified, such as password login or password reset

code:
  the user-submitted captcha code

challengeId:
  image captcha key, one-time challenge id, local fixed challenge id, or flow id

target:
  phone number, email, login name, user id, or another business target
```

For current local password login captcha, use:

```kotlin
VerifyCaptchaCommand(
    purpose = CaptchaPurpose.PASSWORD_LOGIN,
    challengeId = "local",
    code = "8888",
)
```

Captcha issuing and verification should be separated as application interfaces:

```kotlin
interface CaptchaIssuer {
    fun issue(command: IssueCaptchaCommand): IssuedCaptcha
}

interface CaptchaVerifier {
    fun verify(command: VerifyCaptchaCommand)
}
```

Issuing and verification have different responsibilities:

```text
Issuing:
  generate captcha
  store expected answer
  return challengeId, image or challenge metadata, and expiration

Verification:
  check challengeId or target with captcha code
  consume the captcha when appropriate
  handle invalid, expired, or already-used captcha
```

The password login use case should depend only on `CaptchaVerifier`, because it only verifies a submitted captcha. It should not depend on the issuing capability.

The HTTP API should also keep these operations separate:

```text
GET  /iam/captchas/password-login
POST /iam/auth/login
```

The issuing endpoint is called when the client needs a captcha challenge. The login endpoint receives `loginName`, `password`, `captchaChallengeId`, and `captchaCode`, then verifies the captcha before authenticating the password.

The underlying infrastructure implementation can still share storage and helper components:

```text
iam.infrastructure.captcha
  CaptchaStore
  RedisCaptchaIssuer
  RedisCaptchaVerifier
  FixedLocalCaptchaVerifier
```

For the current local-only fixed captcha stage, it is acceptable to implement only `CaptchaVerifier`. A local `CaptchaIssuer` can be added later if the frontend requires a captcha key or challenge response.

For the local profile, use a fixed-code implementation:

```text
iam.infrastructure.captcha.FixedLocalCaptchaVerifier
```

The local verifier can accept `8888` as the valid captcha code. This implementation must be local-only and should not be active in production-like profiles.

Future production implementations can be added without changing the login use case:

```text
iam.infrastructure.captcha.RedisCaptchaVerifier
iam.interfaces.web.CaptchaController
```

## Multiple Captcha Types Later

If the project later adds image, SMS, and email captcha, do not immediately introduce `CaptchaProvider`, `CaptchaResolver`, or `CaptchaVerifierComposite`.

Prefer making the business intent explicit in the command:

```kotlin
enum class CaptchaPurpose {
    PASSWORD_LOGIN,
    PASSWORD_RESET,
    CHANGE_EMAIL,
}
```

The verifier can branch by purpose when the complexity is still small. This keeps the call site explicit and avoids resolver chains that guess the intended captcha type.

Use `purpose` before `channel` unless there is a clear reason not to. Business rules are usually attached to purpose. For example, password login, password reset, and changing email may all use codes, but they can have different expiration, retry, and risk-control rules.

## Authentication Converter Request Matching

Keep the previous custom converter abstraction if multiple Spring Security authentication converters are expected:

```kotlin
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.security.web.util.matcher.RequestMatcher

interface IamAuthenticationConverter : AuthenticationConverter {
    val requestMatcher: RequestMatcher
}
```

This is a useful infrastructure-level abstraction because an authentication converter and its applicable request matcher are a coupled pair. Keeping them together avoids duplicating the same path and method in both the converter and the security filter-chain configuration.

Place it under the Spring Security infrastructure package:

```text
iam.infrastructure.security.authentication.converter
  IamAuthenticationConverter
  PasswordLoginAuthenticationConverter
```

Example usage:

```kotlin
@Component
class PasswordLoginAuthenticationConverter(
    private val objectMapper: ObjectMapper,
) : IamAuthenticationConverter {
    override val requestMatcher: RequestMatcher =
        AntPathRequestMatcher("/iam/auth/login", "POST")

    override fun convert(request: HttpServletRequest): Authentication? {
        if (!requestMatcher.matches(request)) {
            return null
        }

        // Parse request body and return an unauthenticated token.
    }
}
```

The security configuration can then collect all IAM converters and compose a request matcher from them:

```kotlin
class IamSecurityConfiguration(
    private val converters: List<IamAuthenticationConverter>,
) {
    fun authenticationFilter(authenticationManager: AuthenticationManager): AuthenticationFilter {
        val matcher = OrRequestMatcher(converters.map { it.requestMatcher })
        val converter = DelegatingAuthenticationConverter(converters)

        return AuthenticationFilter(authenticationManager, converter).apply {
            setRequestMatcher(matcher)
        }
    }
}
```

Design constraints:

```text
The converter should still check its own requestMatcher and return null when it does not match.
The requestMatcher should only describe the HTTP entry point.
Do not put authorization, captcha, or login business rules into the requestMatcher.
Do not introduce converter registries, providers, or resolver chains unless real complexity appears.
```

This abstraction should remain inside `iam.infrastructure.security`. It should not be used as an IAM application or domain abstraction.

## Direct Spring Security Login Flow

The password login entry point should directly use Spring Security instead of an application-level `PasswordLoginService`.

Do not keep a parallel controller login flow. The login request should be owned by the Spring Security filter chain:

```text
POST /iam/auth/login
  -> AuthenticationFilter
  -> PasswordLoginAuthenticationConverter
  -> PasswordLoginAuthenticationToken
  -> PasswordLoginAuthenticationProvider
  -> SuccessHandler / FailureHandler
```

With this approach, these application-level login DTOs and service types are not needed:

```text
PasswordLoginService
PasswordLoginCommand
LoginResult
AuthController for login
```

The provider orchestrates the password-login authentication flow:

```text
PasswordLoginAuthenticationProvider.authenticate(...)
  1. Read loginName, password, and captcha fields from the token.
  2. Verify captcha.
  3. Normalize loginName.
  4. Find identity.
  5. Find user.
  6. Check user status.
  7. Find password credential.
  8. Verify password.
  9. Return an authenticated token.
```

This does not mean all rules should be written directly inside the provider. The provider should orchestrate the flow and delegate concrete rules to focused components:

```text
CaptchaVerifier:
  captcha validation

LoginNameNormalizer:
  login name parsing and normalization

LoginRiskChecker:
  optional future risk-control checks

LoginAttemptRecorder:
  optional future success/failure recording

PasswordEncoder and CredentialRepository:
  password credential verification
```

Do not introduce a custom IAM provider base class before it is needed. Start with:

```kotlin
class PasswordLoginAuthenticationProvider : AuthenticationProvider
```

If a second or third provider appears later, such as SMS login or email login, then extract a small typed base class:

```kotlin
abstract class IamAuthenticationProvider<T : Authentication>(
    private val tokenType: Class<T>,
) : AuthenticationProvider {
    final override fun supports(authentication: Class<*>): Boolean =
        tokenType.isAssignableFrom(authentication)

    final override fun authenticate(authentication: Authentication): Authentication =
        authenticateTyped(tokenType.cast(authentication))

    protected abstract fun authenticateTyped(authentication: T): Authentication
}
```

This keeps the initial password-login implementation simple while still leaving a clean path for future provider reuse.

## Current Recommendation

Use the recommended package structure above.

Keep password login with captcha verification as the main login flow.

Implement captcha issuing and verification as separate application abstractions when both are needed. The password login use case should depend on `CaptchaVerifier`.

Use local fixed captcha code `8888` only under the local profile.

Model captcha verification with `challengeId` and `target` instead of a permanently required `key`.

Avoid building a full captcha generation, Redis storage, expiration, and one-time-consumption flow until production captcha behavior is needed.

Keep `IamAuthenticationConverter` as a small Spring Security infrastructure abstraction when it reduces duplicated request matcher configuration.

When using direct Spring Security login, let `PasswordLoginAuthenticationProvider` orchestrate the password-login flow and delegate concrete rules to focused components.
