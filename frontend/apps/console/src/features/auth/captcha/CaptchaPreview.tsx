import type { IssuedCaptcha } from "@/features/auth/login/api";

type CaptchaPreviewProps = {
  captcha: IssuedCaptcha | null;
  imageSrc: string | null;
  loading: boolean;
  remainingSeconds: number;
  onRefresh: () => void;
};

export function CaptchaPreview({
  captcha,
  imageSrc,
  loading,
  remainingSeconds,
  onRefresh,
}: CaptchaPreviewProps) {
  if (!captcha) {
    return (
      <div className="mb-4 flex h-10 items-center rounded border border-dashed border-slate-200 bg-slate-50 px-3 text-sm text-slate-400">
        验证码加载中
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
          <img alt="验证码" className="h-full max-w-full object-contain" src={imageSrc} />
        ) : (
          <span className="text-sm text-slate-400">验证码响应格式不正确</span>
        )}
      </button>
    );
  }

  if (captcha.delivery === "EMAIL") {
    return (
      <div className="mb-4 flex h-10 items-center rounded border border-slate-200 bg-slate-50 px-3 text-sm text-slate-500">
        {remainingSeconds > 0
          ? `邮箱验证码已发送，${remainingSeconds} 秒后可重新发送`
          : "邮箱验证码已发送"}
      </div>
    );
  }

  return (
    <div className="mb-4 flex h-10 items-center justify-center rounded border border-slate-200 bg-slate-50 font-mono text-sm font-semibold text-slate-700">
      8888
    </div>
  );
}
