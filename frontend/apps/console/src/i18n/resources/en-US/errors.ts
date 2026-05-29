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
      invalid_captcha: "Incorrect captcha. Try again",
      invalid_credentials: "Incorrect account or password",
      user_disabled: "This user is disabled",
    },
  },
} as const;
