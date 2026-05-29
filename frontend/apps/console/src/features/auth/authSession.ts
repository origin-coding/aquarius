import { authTokenStore, canRestoreAuthSession, useAuthStore } from "@/features/auth/authStore";

type AuthExpiredHandler = () => void | Promise<void>;

let bootstrapPromise: Promise<void> | null = null;
let authExpiredHandler: AuthExpiredHandler | null = null;

export function configureAuthSession(handler: AuthExpiredHandler): void {
  authExpiredHandler = handler;
}

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
  bootstrapPromise = null;
  await authTokenStore.clearTokens();
  useAuthStore.getState().clear();
}

export async function expireAuthentication(): Promise<void> {
  await clearAuthentication();
  await authExpiredHandler?.();
}
