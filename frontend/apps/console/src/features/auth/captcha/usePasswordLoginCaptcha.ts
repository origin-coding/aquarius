import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { useCountdown } from "@/features/auth/captcha/useCountdown";
import { issuePasswordLoginCaptcha, type IssuedCaptcha } from "@/features/auth/login/api";
import { translateApiError } from "@/shared/api/apiErrorMessages";

export function usePasswordLoginCaptcha(loginName: string | undefined) {
  const [captcha, setCaptcha] = useState<IssuedCaptcha | null>(null);
  const [captchaExpiresAt, setCaptchaExpiresAt] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const requestIdRef = useRef(0);
  const remainingSeconds = useCountdown(captchaExpiresAt);
  const normalizedLoginName = useMemo(() => loginName?.trim() ?? "", [loginName]);

  const imageSrc = useMemo(() => {
    if (captcha?.delivery !== "IMAGE" || !captcha.imageBase64) {
      return null;
    }

    return `data:${captcha.imageContentType ?? "image/png"};base64,${captcha.imageBase64}`;
  }, [captcha]);

  const clearCaptcha = useCallback(() => {
    setCaptcha(null);
    setCaptchaExpiresAt(null);
  }, []);

  const loadCaptcha = useCallback(async (requestLoginName: string, requestId: number) => {
    setLoading(true);
    setErrorCode(null);

    try {
      const nextCaptcha = await issuePasswordLoginCaptcha(requestLoginName);
      if (requestId !== requestIdRef.current) {
        return;
      }

      setCaptcha(nextCaptcha);
      setCaptchaExpiresAt(
        typeof nextCaptcha.expiresIn === "number"
          ? Date.now() + nextCaptcha.expiresIn * 1000
          : null,
      );
    } catch (error) {
      if (requestId !== requestIdRef.current) {
        return;
      }

      clearCaptcha();
      setErrorCode(translateApiError(error, "captcha.issue_failed"));
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [clearCaptcha]);

  const refreshCaptcha = useCallback(async () => {
    const requestId = requestIdRef.current + 1;
    requestIdRef.current = requestId;

    if (!normalizedLoginName) {
      clearCaptcha();
      setErrorCode(null);
      setLoading(false);
      return;
    }

    await loadCaptcha(normalizedLoginName, requestId);
  }, [clearCaptcha, loadCaptcha, normalizedLoginName]);

  useEffect(() => {
    const requestId = requestIdRef.current + 1;
    requestIdRef.current = requestId;

    clearCaptcha();
    setErrorCode(null);

    if (!normalizedLoginName) {
      setLoading(false);
      return undefined;
    }

    setLoading(true);
    const timeoutId = window.setTimeout(() => {
      void loadCaptcha(normalizedLoginName, requestId);
    }, 350);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [clearCaptcha, loadCaptcha, normalizedLoginName]);

  return {
    captcha,
    captchaChallengeId: captcha?.captchaChallengeId ?? null,
    delivery: captcha?.delivery ?? null,
    errorCode,
    imageSrc,
    isExpired: captcha !== null && captchaExpiresAt !== null && remainingSeconds === 0,
    loading,
    remainingSeconds,
    requiresLoginName: !normalizedLoginName,
    refreshCaptcha,
    setErrorCode,
  };
}
