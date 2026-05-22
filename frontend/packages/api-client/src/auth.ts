import dayjs from "dayjs";

export type MaybePromise<T> = T | Promise<T>;

export type AuthTokens = {
  accessToken: string;
  refreshToken?: string;
  accessTokenExpiresAt?: number;
  refreshTokenExpiresAt?: number;
};

export type AuthTokenState = {
  accessToken: string | null;
  refreshToken: string | null;
  accessTokenExpiresAt: number | null;
  refreshTokenExpiresAt: number | null;
};

export type AuthTokenStateInput = Partial<AuthTokenState>;

export type AuthExpiredReason = {
  cause: "refresh-failed" | "refresh-unavailable" | "retry-failed";
  response?: Response;
  error?: unknown;
};

export type AuthTokenStore = {
  getAccessToken(): MaybePromise<string | null>;
  setAccessToken(token: string | null): MaybePromise<void>;
  getRefreshToken(): MaybePromise<string | null>;
  setRefreshToken(token: string | null): MaybePromise<void>;
  getTokenState?(): MaybePromise<AuthTokenState>;
  setTokenState?(state: AuthTokenStateInput): MaybePromise<void>;
  clearTokens(): MaybePromise<void>;
};

export type RefreshAccessToken = () => Promise<AuthTokens | null>;

export function createMemoryAuthTokenStore(
  initialTokens: AuthTokenStateInput = {},
): AuthTokenStore {
  let tokenState: AuthTokenState = normalizeTokenState(initialTokens);

  return {
    getAccessToken: () => tokenState.accessToken,
    setAccessToken: (token) => {
      tokenState = {
        ...tokenState,
        accessToken: token,
        accessTokenExpiresAt: token ? tokenState.accessTokenExpiresAt : null,
      };
    },
    getRefreshToken: () => tokenState.refreshToken,
    setRefreshToken: (token) => {
      tokenState = {
        ...tokenState,
        refreshToken: token,
        refreshTokenExpiresAt: token ? tokenState.refreshTokenExpiresAt : null,
      };
    },
    getTokenState: () => ({ ...tokenState }),
    setTokenState: (state) => {
      tokenState = normalizeTokenState({
        ...tokenState,
        ...state,
      });
    },
    clearTokens: () => {
      tokenState = normalizeTokenState();
    },
  };
}

export function tokenExpiresAt(expiresInSeconds: number, now = dayjs()): number {
  return now.add(expiresInSeconds, "second").valueOf();
}

export function createSingleFlightRefresh(
  refreshAccessToken: RefreshAccessToken,
): RefreshAccessToken {
  let refreshPromise: Promise<AuthTokens | null> | null = null;

  return async () => {
    refreshPromise ??= refreshAccessToken().finally(() => {
      refreshPromise = null;
    });

    return refreshPromise;
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
