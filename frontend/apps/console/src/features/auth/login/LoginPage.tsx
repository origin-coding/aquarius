import {
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Alert, Button, Form, Input, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";

import loginBackgroundUrl from "@/assets/login-background-light.png";
import { usePasswordLoginMutation } from "@/features/auth/authMutations";
import { CaptchaPreview } from "@/features/auth/captcha/CaptchaPreview";
import { usePasswordLoginCaptcha } from "@/features/auth/captcha/usePasswordLoginCaptcha";
import type { PasswordLoginRequest } from "@/features/auth/login/api";

const { Text, Title } = Typography;

const loginContentClassName = "w-full max-w-[390px] lg:ml-8 xl:ml-12";

export function LoginPage() {
  return (
    <main className="min-h-screen overflow-hidden bg-white text-slate-950">
      <div className="relative min-h-screen">
        <section className="pointer-events-none absolute inset-y-0 right-0 hidden w-[68%] lg:block">
          <img
            alt=""
            className="h-full w-full object-cover object-center"
            src={loginBackgroundUrl}
          />
          <div className="absolute inset-0 bg-gradient-to-r from-white via-white/80 to-white/5" />
        </section>

        <section className="relative z-10 flex min-h-screen w-full flex-col px-6 py-8 sm:px-12 lg:w-[600px] lg:px-20 xl:px-24">
          <div className={`${loginContentClassName} flex items-center gap-4`}>
            <div className="h-11 w-11 rounded-lg bg-[#1677ff] text-white flex items-center justify-center text-lg font-semibold shadow-sm shadow-blue-200">
              A
            </div>
            <Text className="block text-xl text-slate-950 font-semibold">
              Aquarius Console
            </Text>
          </div>

          <div className="flex flex-1 items-center py-12">
            <div className={loginContentClassName}>
              <Title level={1} className="!mb-3 !text-[36px] !leading-tight">
                登录控制台
              </Title>
              <Text className="block text-slate-500">使用账号和密码登录</Text>

              <div className="mt-9">
                <PasswordLoginForm />
              </div>
            </div>
          </div>

          <footer className={loginContentClassName}>
            <Text className="text-slate-400 text-sm">
              © 2026 Origin Coding. All rights reserved.
            </Text>
          </footer>
        </section>
      </div>
    </main>
  );
}

function PasswordLoginForm() {
  const [form] = Form.useForm<PasswordLoginRequest>();
  const loginMutation = usePasswordLoginMutation();
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const {
    captcha,
    captchaChallengeId,
    delivery,
    errorCode: captchaErrorCode,
    imageSrc,
    loading: captchaLoading,
    remainingSeconds,
    refreshCaptcha: refreshPasswordLoginCaptcha,
    setErrorCode: setCaptchaErrorCode,
  } = usePasswordLoginCaptcha();
  const displayedErrorCode = errorCode ?? captchaErrorCode;

  const refreshCaptcha = useCallback(async () => {
    setErrorCode(null);
    await refreshPasswordLoginCaptcha();
  }, [refreshPasswordLoginCaptcha]);

  useEffect(() => {
    if (!captchaChallengeId) {
      return;
    }

    form.setFieldsValue({
      captchaChallengeId,
      captchaCode: "",
    });
  }, [form, captchaChallengeId]);

  const handleSubmit = async (values: PasswordLoginRequest) => {
    setErrorCode(null);
    setCaptchaErrorCode(null);

    try {
      await loginMutation.mutateAsync({
        ...values,
        loginName: values.loginName.trim(),
        captchaCode: values.captchaCode.trim(),
      });
    } catch (error) {
      setErrorCode(error instanceof Error ? error.message : "auth.login_failed");
      void refreshPasswordLoginCaptcha();
    }
  };

  return (
    <Form<PasswordLoginRequest>
      form={form}
      layout="vertical"
      requiredMark={false}
      onFinish={handleSubmit}
    >
      {displayedErrorCode ? (
        <Alert
          className="!mb-4"
          title={errorMessage(displayedErrorCode)}
          showIcon
          type="error"
        />
      ) : null}

      <Form.Item name="loginName" rules={[{ required: true, message: "请输入账号" }]}>
        <Input
          autoComplete="username"
          placeholder="请输入账号或邮箱"
          prefix={<UserOutlined className="text-slate-400" />}
          size="large"
        />
      </Form.Item>

      <Form.Item name="password" rules={[{ required: true, message: "请输入密码" }]}>
        <Input.Password
          autoComplete="current-password"
          placeholder="请输入密码"
          prefix={<LockOutlined className="text-slate-400" />}
          size="large"
        />
      </Form.Item>

      <Form.Item name="captchaChallengeId" hidden>
        <Input />
      </Form.Item>

      <Form.Item required>
        <div className="grid grid-cols-[minmax(0,1fr)_112px] gap-3">
          <Form.Item
            name="captchaCode"
            noStyle
            rules={[{ required: true, message: "请输入验证码" }]}
          >
            <Input
              autoComplete="off"
              placeholder="请输入验证码"
              prefix={<SafetyCertificateOutlined className="text-slate-400" />}
              size="large"
            />
          </Form.Item>
          <Button
            className="!h-10"
            icon={<ReloadOutlined />}
            disabled={delivery === "EMAIL" && remainingSeconds > 0}
            loading={captchaLoading}
            onClick={refreshCaptcha}
          >
            {delivery === "EMAIL" && remainingSeconds > 0
              ? `${remainingSeconds}s`
              : "刷新"}
          </Button>
        </div>
      </Form.Item>

      <CaptchaPreview
        captcha={captcha}
        imageSrc={imageSrc}
        loading={captchaLoading}
        remainingSeconds={remainingSeconds}
        onRefresh={refreshCaptcha}
      />

      <Button
        block
        disabled={!captcha}
        htmlType="submit"
        loading={loginMutation.isPending}
        size="large"
        type="primary"
      >
        登录控制台
      </Button>
    </Form>
  );
}

function errorMessage(code: string): string {
  switch (code) {
    case "iam.auth.invalid_captcha":
      return "验证码错误，请重新输入";
    case "iam.auth.invalid_credentials":
      return "账号或密码错误";
    case "iam.auth.user_disabled":
      return "当前用户已被禁用";
    case "captcha.issue_failed":
      return "验证码加载失败";
    case "captcha.issue_invalid_response":
      return "验证码响应格式不正确";
    case "auth.login_invalid_response":
      return "登录响应格式不正确";
    case "auth.login_failed":
      return "登录失败，请稍后再试";
    default:
      return code;
  }
}
