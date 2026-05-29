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
      access_denied: "没有权限访问该资源",
      authentication_failed: "认证失败，请重新登录",
      invalid_captcha: "验证码错误，请重新输入",
      invalid_credentials: "账号或密码错误",
      invalid_refresh_token: "登录状态已过期，请重新登录",
      unauthenticated: "请先登录",
      user_disabled: "当前用户已被禁用",
    },
  },
  operation: {
    not_allowed: "当前操作不允许",
  },
  request: {
    malformed: "请求格式不正确",
    method_not_allowed: "请求方法不支持",
    payload_too_large: "请求内容过大",
    unsupported_media_type: "请求内容类型不支持",
    validation_failed: "请求参数校验失败",
  },
  resource: {
    conflict: "资源状态冲突",
    not_found: "资源不存在",
  },
  system: {
    internal: "系统内部错误",
  },
  upstream: {
    unavailable: "上游服务暂不可用",
  },
  validation: {
    decimal_min: "数值不能小于 {{value}}",
    decimal_max: "数值不能大于 {{value}}",
    email: {
      invalid: "邮箱格式不正确",
    },
    invalid: "字段格式不正确",
    max: "数值不能大于 {{value}}",
    min: "数值不能小于 {{value}}",
    negative: "请输入负数",
    negative_or_zero: "请输入小于或等于 0 的数值",
    pattern: {
      invalid: "字段格式不正确",
    },
    positive: "请输入正数",
    positive_or_zero: "请输入大于或等于 0 的数值",
    required: "必填项不能为空",
    size: {
      invalid: "长度不符合要求",
    },
  },
} as const;
