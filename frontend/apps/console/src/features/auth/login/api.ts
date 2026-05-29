import type {components} from "@aquarius/api-types/generated/schema";

import {api, apiErrorCode, readApiData} from "@/features/auth/apiClient";
import type {LoginSuccessResponse} from "@/features/auth/authTypes";

export type PasswordLoginRequest = components["schemas"]["PasswordLoginRequest"];
export type IssuedCaptcha = components["schemas"]["IssuedCaptcha"];

export async function issuePasswordLoginCaptcha(): Promise<IssuedCaptcha> {
  const { data, error } = await api.GET("/iam/captchas/password-login");

  if (error) {
    throw new Error(apiErrorCode(error, "captcha.issue_failed"));
  }

  const captcha = readApiData(data, "captcha.issue_invalid_response");
  if (!captcha.captchaChallengeId) {
    throw new Error("captcha.issue_invalid_response");
  }

  if (captcha.delivery === "IMAGE" && !captcha.imageBase64) {
    throw new Error("captcha.issue_invalid_response");
  }

  return captcha;
}

export async function loginWithPassword(
  request: PasswordLoginRequest,
): Promise<LoginSuccessResponse> {
  const { data, error } = await api.POST("/iam/auth/sessions/password", {
    body: request,
  });

  if (error) {
    throw new Error(apiErrorCode(error, "auth.login_failed"));
  }

  return readApiData(data, "auth.login_invalid_response");
}
