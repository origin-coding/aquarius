import { useTranslation } from "react-i18next";

import type { IssuedCaptcha } from "@/features/auth/login/api";

type CaptchaPreviewProps = {
  captcha: IssuedCaptcha | null;
  imageSrc: string | null;
  loading: boolean;
  remainingSeconds: number;
  requiresLoginName: boolean;
  onRefresh: () => void;
};

export function CaptchaPreview({
  captcha,
  imageSrc,
  loading,
  remainingSeconds,
  requiresLoginName,
  onRefresh,
}: CaptchaPreviewProps) {
  const { t } = useTranslation();

  if (!captcha) {
    return (
      <div className="mb-4 flex h-10 items-center rounded border border-dashed border-slate-200 bg-slate-50 px-3 text-sm text-slate-400">
        {requiresLoginName && !loading
          ? t(($) => $.captcha.waitingForLoginName)
          : t(($) => $.captcha.loading)}
      </div>
    );
  }

  if (captcha.delivery === "IMAGE") {
    return (
      <button
        className="mb-4 flex h-10 w-full items-center justify-center overflow-hidden rounded border border-slate-200 bg-white hover:border-[#1677ff] disabled:cursor-not-allowed disabled:opacity-60"
        disabled={loading}
        onClick={onRefresh}
        type="button"
      >
        {imageSrc ? (
          <img
            alt={t(($) => $.captcha.alt)}
            className="h-full max-w-full object-contain"
            src={imageSrc}
          />
        ) : (
          <span className="text-sm text-slate-400">{t(($) => $.captcha.invalidResponse)}</span>
        )}
      </button>
    );
  }

  if (captcha.delivery === "EMAIL") {
    return (
      <div className="mb-4 flex h-10 items-center rounded border border-slate-200 bg-slate-50 px-3 text-sm text-slate-500">
        {remainingSeconds > 0
          ? t(($) => $.captcha.emailSentWithCountdown, { seconds: remainingSeconds })
          : t(($) => $.captcha.emailSent)}
      </div>
    );
  }

  return (
    <div className="mb-4 flex h-10 items-center justify-center rounded border border-slate-200 bg-slate-50 font-mono text-sm font-semibold text-slate-700">
      {t(($) => $.captcha.developmentCode)}
    </div>
  );
}
