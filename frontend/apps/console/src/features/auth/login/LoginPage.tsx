import {
  GlobalOutlined,
  LockOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Alert, Button, Dropdown, Form, Input, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import loginBackgroundUrl from "@/assets/login-background-light.png";
import { usePasswordLoginMutation } from "@/features/auth/authMutations";
import { CaptchaPreview } from "@/features/auth/captcha/CaptchaPreview";
import { usePasswordLoginCaptcha } from "@/features/auth/captcha/usePasswordLoginCaptcha";
import type { PasswordLoginRequest } from "@/features/auth/login/api";
import type { SupportedLocale } from "@/i18n";
import { changeLocale, getCurrentLocale } from "@/i18n/locale";

const { Text, Title } = Typography;

const loginContentClassName = "w-full max-w-[390px] lg:ml-8 xl:ml-12";

export function LoginPage() {
  const { t } = useTranslation();

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
          <div className={`${loginContentClassName} flex items-center justify-between gap-4`}>
            <div className="flex items-center gap-4">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-[#1677ff] text-lg font-semibold text-white shadow-sm shadow-blue-200">
                A
              </div>
              <Text className="block text-xl font-semibold text-slate-950">
                {t(($) => $.app.console)}
              </Text>
            </div>
            <LoginLocaleSwitcher />
          </div>

          <div className="flex flex-1 items-center py-12">
            <div className={loginContentClassName}>
              <Title level={1} className="!mb-3 !text-[36px] !leading-tight">
                {t(($) => $.login.title)}
              </Title>
              <Text className="block text-slate-500">{t(($) => $.login.subtitle)}</Text>

              <div className="mt-9">
                <PasswordLoginForm />
              </div>
            </div>
          </div>

          <footer className={loginContentClassName}>
            <Text className="text-sm text-slate-400">{t(($) => $.login.copyright)}</Text>
          </footer>
        </section>
      </div>
    </main>
  );
}

function LoginLocaleSwitcher() {
  const { t } = useTranslation();
  const currentLocale = getCurrentLocale();

  const handleLocaleChange = (locale: SupportedLocale) => {
    if (locale !== currentLocale) {
      void changeLocale(locale);
    }
  };

  return (
    <Dropdown
      menu={{
        onClick: ({ key }) => handleLocaleChange(key as SupportedLocale),
        selectedKeys: [currentLocale],
        items: [
          {
            key: "zh-CN",
            label: t(($) => $.language.zhCN),
          },
          {
            key: "en-US",
            label: t(($) => $.language.enUS),
          },
        ],
      }}
      placement="bottomRight"
      trigger={["click"]}
    >
      <Button icon={<GlobalOutlined />} type="text">
        {currentLocale}
      </Button>
    </Dropdown>
  );
}

function PasswordLoginForm() {
  const { t } = useTranslation();
  const { t: tErrors } = useTranslation("errors");
  const { t: tValidation } = useTranslation("validation");
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
  const displayedErrorMessage = displayedErrorCode
    ? (() => {
        switch (displayedErrorCode) {
          case "iam.auth.invalid_captcha":
            return tErrors(($) => $.iam.auth.invalid_captcha);
          case "iam.auth.invalid_credentials":
            return tErrors(($) => $.iam.auth.invalid_credentials);
          case "iam.auth.user_disabled":
            return tErrors(($) => $.iam.auth.user_disabled);
          case "captcha.issue_failed":
            return tErrors(($) => $.captcha.issue_failed);
          case "captcha.issue_invalid_response":
            return tErrors(($) => $.captcha.issue_invalid_response);
          case "auth.login_invalid_response":
            return tErrors(($) => $.auth.login_invalid_response);
          case "auth.login_failed":
            return tErrors(($) => $.auth.login_failed);
          default:
            return displayedErrorCode;
        }
      })()
    : null;

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
      {displayedErrorMessage ? (
        <Alert className="!mb-4" title={displayedErrorMessage} showIcon type="error" />
      ) : null}

      <Form.Item
        name="loginName"
        rules={[{ required: true, message: tValidation(($) => $.loginName.required) }]}
      >
        <Input
          autoComplete="username"
          placeholder={tValidation(($) => $.loginName.placeholder)}
          prefix={<UserOutlined className="text-slate-400" />}
          size="large"
        />
      </Form.Item>

      <Form.Item
        name="password"
        rules={[{ required: true, message: tValidation(($) => $.password.required) }]}
      >
        <Input.Password
          autoComplete="current-password"
          placeholder={tValidation(($) => $.password.placeholder)}
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
            rules={[{ required: true, message: tValidation(($) => $.captchaCode.required) }]}
          >
            <Input
              autoComplete="off"
              placeholder={tValidation(($) => $.captchaCode.placeholder)}
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
              : t(($) => $.actions.refresh)}
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
        {t(($) => $.login.submit)}
      </Button>
    </Form>
  );
}
