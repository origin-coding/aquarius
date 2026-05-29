import { authTokenStore, canRestoreAuthSession, useAuthStore } from "@/features/auth/authStore";

let bootstrapPromise: Promise<void> | null = null;

export function bootstrapAuthState(): Promise<void> {
  bootstrapPromise ??= doBootstrapAuthState();
  return bootstrapPromise;
}

async function doBootstrapAuthState(): Promise<void> {
  const state = useAuthStore.getState();
  const tokenState = (await authTokenStore.getTokenState?.()) ?? state.tokenState;
  const user = state.user;

  if (user && canRestoreAuthSession(tokenState)) {
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
