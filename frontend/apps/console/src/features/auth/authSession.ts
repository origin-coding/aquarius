import { authTokenStore, useAuthStore } from "@/features/auth/authStore";

let bootstrapPromise: Promise<void> | null = null;

export function bootstrapAuthState(): Promise<void> {
  bootstrapPromise ??= doBootstrapAuthState();
  return bootstrapPromise;
}

async function doBootstrapAuthState(): Promise<void> {
  const tokenState = await authTokenStore.getTokenState?.();
  const user = useAuthStore.getState().user;

  if ((tokenState?.accessToken || tokenState?.refreshToken) && user) {
    useAuthStore.getState().setAuthenticated(user);
    return;
  }

  await authTokenStore.clearTokens();
  useAuthStore.getState().setAnonymous();
}

export async function clearAuthentication(): Promise<void> {
  await authTokenStore.clearTokens();
  useAuthStore.getState().clear();
}
