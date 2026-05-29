import { createAquariusApiClient, getApiErrorCode } from "@aquarius/api-client";

import { clearAuthentication } from "@/features/auth/authSession";
import { authTokenStore } from "@/features/auth/authStore";

type ApiDataResponse<T> = {
  data?: T;
};

export const api = createAquariusApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? "/api",
  tokenStore: authTokenStore,
  onAuthExpired: async () => {
    await clearAuthentication();
  },
});

export function apiErrorCode(error: unknown, fallback: string): string {
  return getApiErrorCode(error) ?? fallback;
}

export function readApiData<T>(
  response: ApiDataResponse<T> | undefined,
  fallbackErrorCode: string,
): NonNullable<T> {
  if (response?.data === undefined || response.data === null) {
    throw new Error(fallbackErrorCode);
  }

  return response.data;
}
