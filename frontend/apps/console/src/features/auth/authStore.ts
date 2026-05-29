import { useStore } from "zustand";
import { persist } from "zustand/middleware";
import { createStore, type StoreApi } from "zustand/vanilla";
import type {
  AuthTokenState,
  AuthTokenStateInput,
  AuthTokenStore,
} from "@aquarius/api-client";

import type { AuthStatus, CurrentUser } from "@/features/auth/authTypes";

const authStorageKey = "aquarius.console.auth";
const legacyUserStorageKey = "aquarius.console.auth.user";
const legacyTokenStorageKey = "aquarius.console.auth.tokens";

type AuthState = {
  status: AuthStatus;
  user: CurrentUser | null;
  tokenState: AuthTokenState;
  setAuthenticated: (user: CurrentUser) => void;
  setAnonymous: () => void;
  setTokenState: (state: AuthTokenStateInput) => void;
  clearTokens: () => void;
  clear: () => void;
  hasAuthority: (authority: string) => boolean;
};

type PersistedAuthState = Pick<AuthState, "user" | "tokenState">;
type AuthStore = StoreApi<AuthState>;
type UseAuthStore = {
  (): AuthState;
  <T>(selector: (state: AuthState) => T): T;
} & AuthStore;

export const authStore = createStore<AuthState>()(
  persist(
    (set, get) => ({
      status: "unknown",
      user: readLegacyUser(),
      tokenState: readLegacyTokenState(),

      setAuthenticated: (user) => {
        clearLegacyAuthStorage();
        set({
          status: "authenticated",
          user,
        });
      },

      setAnonymous: () => {
        clearLegacyAuthStorage();
        set({
          status: "anonymous",
          user: null,
          tokenState: emptyTokenState(),
        });
      },

      setTokenState: (state) => {
        clearLegacyAuthStorage();
        set((currentState) => ({
          tokenState: normalizeTokenState({
            ...currentState.tokenState,
            ...state,
          }),
        }));
      },

      clearTokens: () => {
        clearLegacyAuthStorage();
        set({
          tokenState: emptyTokenState(),
        });
      },

      clear: () => {
        clearLegacyAuthStorage();
        set({
          status: "anonymous",
          user: null,
          tokenState: emptyTokenState(),
        });
      },

      hasAuthority: (authority) => {
        return get().user?.authorities.includes(authority) ?? false;
      },
    }),
    {
      name: authStorageKey,
      partialize: (state): PersistedAuthState => ({
        user: state.user,
        tokenState: state.tokenState,
      }),
      merge: (persistedState, currentState) => {
        const persistedAuthState = persistedState as PersistedAuthState | undefined;
        const user = persistedAuthState?.user ?? currentState.user;
        const tokenState = normalizeTokenState(
          persistedAuthState?.tokenState ?? currentState.tokenState,
        );

        return {
          ...currentState,
          status: user && hasTokens(tokenState) ? "authenticated" : currentState.status,
          user,
          tokenState,
        };
      },
    },
  ),
);

export const authTokenStore: AuthTokenStore = {
  getAccessToken: () => authStore.getState().tokenState.accessToken,

  setAccessToken: (accessToken) => {
    const currentTokenState = authStore.getState().tokenState;

    authStore.getState().setTokenState({
      accessToken,
      accessTokenExpiresAt: accessToken ? currentTokenState.accessTokenExpiresAt : null,
    });
  },

  getRefreshToken: () => authStore.getState().tokenState.refreshToken,

  setRefreshToken: (refreshToken) => {
    const currentTokenState = authStore.getState().tokenState;

    authStore.getState().setTokenState({
      refreshToken,
      refreshTokenExpiresAt: refreshToken ? currentTokenState.refreshTokenExpiresAt : null,
    });
  },

  getTokenState: () => authStore.getState().tokenState,

  setTokenState: (state) => {
    authStore.getState().setTokenState(state);
  },

  clearTokens: () => {
    authStore.getState().clearTokens();
  },
};

function useAuthStoreHook(): AuthState;
function useAuthStoreHook<T>(selector: (state: AuthState) => T): T;
function useAuthStoreHook<T>(selector?: (state: AuthState) => T): AuthState | T {
  if (selector) {
    return useStore(authStore, selector);
  }

  return useStore(authStore);
}

export const useAuthStore = Object.assign(useAuthStoreHook, authStore) as UseAuthStore;

export function expiresAt(expiresInSeconds: number): number {
  return Date.now() + expiresInSeconds * 1000;
}

function emptyTokenState(): AuthTokenState {
  return {
    accessToken: null,
    refreshToken: null,
    accessTokenExpiresAt: null,
    refreshTokenExpiresAt: null,
  };
}

function normalizeTokenState(state: AuthTokenStateInput = {}): AuthTokenState {
  return {
    accessToken: state.accessToken ?? null,
    refreshToken: state.refreshToken ?? null,
    accessTokenExpiresAt: state.accessTokenExpiresAt ?? null,
    refreshTokenExpiresAt: state.refreshTokenExpiresAt ?? null,
  };
}

function hasTokens(state: AuthTokenState): boolean {
  return Boolean(state.accessToken || state.refreshToken);
}

function readLegacyUser(): CurrentUser | null {
  const raw = readLocalStorageItem(legacyUserStorageKey);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as
      | CurrentUser
      | { state?: { user?: CurrentUser | null } }
      | null;

    if (!parsed || typeof parsed !== "object") {
      return null;
    }

    if ("state" in parsed) {
      return parsed.state?.user ?? null;
    }

    return isCurrentUser(parsed) ? parsed : null;
  } catch {
    removeLocalStorageItem(legacyUserStorageKey);
    return null;
  }
}

function isCurrentUser(value: unknown): value is CurrentUser {
  return (
    typeof value === "object" &&
    value !== null &&
    "id" in value &&
    typeof value.id === "string" &&
    "authorities" in value &&
    Array.isArray(value.authorities)
  );
}

function readLegacyTokenState(): AuthTokenState {
  const raw = readLocalStorageItem(legacyTokenStorageKey);
  if (!raw) {
    return emptyTokenState();
  }

  try {
    return normalizeTokenState(JSON.parse(raw) as AuthTokenStateInput);
  } catch {
    removeLocalStorageItem(legacyTokenStorageKey);
    return emptyTokenState();
  }
}

function clearLegacyAuthStorage(): void {
  removeLocalStorageItem(legacyUserStorageKey);
  removeLocalStorageItem(legacyTokenStorageKey);
}

function readLocalStorageItem(key: string): string | null {
  if (typeof localStorage === "undefined") {
    return null;
  }

  return localStorage.getItem(key);
}

function removeLocalStorageItem(key: string): void {
  if (typeof localStorage === "undefined") {
    return;
  }

  localStorage.removeItem(key);
}

