export const errors = {
  auth: {
    login_failed: "Sign-in failed. Try again later",
    login_invalid_response: "Invalid sign-in response",
  },
  captcha: {
    issue_failed: "Failed to load captcha",
    issue_invalid_response: "Invalid captcha response",
  },
  common: {
    unknown: "Unknown error occurred",
  },
  iam: {
    auth: {
      access_denied: "You do not have permission to access this resource",
      authentication_failed: "Authentication failed. Sign in again",
      invalid_captcha: "Incorrect captcha. Try again",
      invalid_credentials: "Incorrect account or password",
      invalid_refresh_token: "Your session has expired. Sign in again",
      unauthenticated: "Sign in first",
      user_disabled: "This user is disabled",
    },
  },
  operation: {
    not_allowed: "This operation is not allowed",
  },
  request: {
    malformed: "The request is malformed",
    method_not_allowed: "The request method is not supported",
    payload_too_large: "The request payload is too large",
    unsupported_media_type: "The request content type is not supported",
    validation_failed: "Request validation failed",
  },
  resource: {
    conflict: "The resource state conflicts with this operation",
    not_found: "Resource not found",
  },
  system: {
    internal: "Internal system error",
  },
  upstream: {
    unavailable: "Upstream service is unavailable",
  },
  validation: {
    decimal_min: "Value must be at least {{value}}",
    decimal_max: "Value must be at most {{value}}",
    email: {
      invalid: "Enter a valid email address",
    },
    invalid: "Invalid field value",
    max: "Value must be at most {{value}}",
    min: "Value must be at least {{value}}",
    negative: "Enter a negative number",
    negative_or_zero: "Enter a number less than or equal to 0",
    pattern: {
      invalid: "Invalid field format",
    },
    positive: "Enter a positive number",
    positive_or_zero: "Enter a number greater than or equal to 0",
    required: "This field is required",
    size: {
      invalid: "Invalid length",
    },
  },
} as const;
