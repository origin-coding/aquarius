import { useCallback, useEffect, useMemo, useState } from "react";

import {
  issuePasswordLoginCaptcha,
  type IssuedCaptcha,
} from "@/features/auth/login/api";
import { useCountdown } from "@/features/auth/captcha/useCountdown";

export function usePasswordLoginCaptcha() {
  const [captcha, setCaptcha] = useState<IssuedCaptcha | null>(null);
  const [captchaExpiresAt, setCaptchaExpiresAt] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const remainingSeconds = useCountdown(captchaExpiresAt);

  const imageSrc = useMemo(() => {
    if (captcha?.delivery !== "IMAGE" || !captcha.imageBase64) {
      return null;
    }

    return `data:${captcha.imageContentType ?? "image/png"};base64,${captcha.imageBase64}`;
  }, [captcha]);

  const refreshCaptcha = useCallback(async () => {
    setLoading(true);
    setErrorCode(null);

    try {
      const nextCaptcha = await issuePasswordLoginCaptcha();
      setCaptcha(nextCaptcha);
      setCaptchaExpiresAt(
        typeof nextCaptcha.expiresIn === "number"
          ? Date.now() + nextCaptcha.expiresIn * 1000
          : null,
      );
    } catch (error) {
      setCaptcha(null);
      setCaptchaExpiresAt(null);
      setErrorCode(error instanceof Error ? error.message : "captcha.issue_failed");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refreshCaptcha();
  }, [refreshCaptcha]);

  return {
    captcha,
    captchaChallengeId: captcha?.captchaChallengeId ?? null,
    delivery: captcha?.delivery ?? null,
    errorCode,
    imageSrc,
    isExpired: captcha !== null && captchaExpiresAt !== null && remainingSeconds === 0,
    loading,
    remainingSeconds,
    refreshCaptcha,
    setErrorCode,
  };
}
