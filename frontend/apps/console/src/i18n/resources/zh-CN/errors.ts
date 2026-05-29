export const errors = {
  auth: {
    login_failed: "登录失败，请稍后再试",
    login_invalid_response: "登录响应格式不正确",
  },
  captcha: {
    issue_failed: "验证码加载失败",
    issue_invalid_response: "验证码响应格式不正确",
  },
  common: {
    unknown: "发生未知错误",
  },
  iam: {
    auth: {
      invalid_captcha: "验证码错误，请重新输入",
      invalid_credentials: "账号或密码错误",
      user_disabled: "当前用户已被禁用",
    },
  },
} as const;
