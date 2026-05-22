import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "@aquarius/api-types/generated/schema";
import {
  createMemoryAuthTokenStore,
  createSingleFlightRefresh,
  tokenExpiresAt,
  type AuthExpiredReason,
  type AuthTokenStore,
  type AuthTokens,
  type MaybePromise,
  type RefreshAccessToken,
} from "./auth";
import { getApiBaseUrl } from "./config";

const REFRESH_TOKEN_PATH = "/iam/auth/sessions/refresh-token";
const PASSWORD_LOGIN_PATH = "/iam/auth/sessions/password";
const LOGOUT_CURRENT_PATH = "/iam/auth/sessions/current";
const LOGOUT_ALL_PATH = "/iam/auth/sessions";

const DEFAULT_REFRESH_EXCLUDED_PATHS = new Set([
  REFRESH_TOKEN_PATH,
  PASSWORD_LOGIN_PATH,
  LOGOUT_CURRENT_PATH,
  LOGOUT_ALL_PATH,
]);

export type AquariusApiClient = ReturnType<typeof createClient<paths>>;

export type CreateApiClientOptions = {
  baseUrl?: string;
  fetch?: typeof fetch;
  tokenStore?: AuthTokenStore;
  refreshAccessToken?: RefreshAccessToken;
  onAuthExpired?: (reason: AuthExpiredReason) => MaybePromise<void>;
  refreshExcludedPaths?: Iterable<string>;
};

type RefreshTokenResponseBody = {
  data?: {
    accessToken?: string;
    refreshToken?: string;
    expiresIn?: number;
    refreshExpiresIn?: number;
  };
};

export function createAquariusApiClient(options: CreateApiClientOptions = {}): AquariusApiClient {
  const baseUrl = getApiBaseUrl(options);
  const fetchImpl = options.fetch ?? globalThis.fetch?.bind(globalThis);

  if (!fetchImpl) {
    throw new Error("No fetch implementation is available.");
  }

  const tokenStore = options.tokenStore ?? createMemoryAuthTokenStore();
  const excludedPaths = new Set(options.refreshExcludedPaths ?? DEFAULT_REFRESH_EXCLUDED_PATHS);
  const refreshAccessToken = createSingleFlightRefresh(
    options.refreshAccessToken ?? createDefaultRefreshAccessToken(baseUrl, fetchImpl, tokenStore),
  );
  const authFetch = createAuthFetch({
    fetchImpl,
    tokenStore,
    refreshAccessToken,
    excludedPaths,
    onAuthExpired: options.onAuthExpired,
  });

  const client = createClient<paths>({
    baseUrl,
    fetch: authFetch,
  });

  const authMiddleware: Middleware = {
    async onRequest({ request }) {
      const accessToken = await tokenStore.getAccessToken();

      if (accessToken) {
        request.headers.set("Authorization", `Bearer ${accessToken}`);
      }

      return request;
    },
  };

  client.use(authMiddleware);

  return client;
}

export const api = createAquariusApiClient();

function shouldAttemptRefresh(request: Request, excludedPaths: Set<string>): boolean {
  const path = new URL(request.url).pathname;
  return !excludedPaths.has(path);
}

type AuthFetchOptions = {
  fetchImpl: typeof fetch;
  tokenStore: AuthTokenStore;
  refreshAccessToken: RefreshAccessToken;
  excludedPaths: Set<string>;
  onAuthExpired?: CreateApiClientOptions["onAuthExpired"];
};

function createAuthFetch(options: AuthFetchOptions): typeof fetch {
  return async (input, init) => {
    const request = new Request(input, init);
    const response = await options.fetchImpl(request.clone());

    if (response.status !== 401 || !shouldAttemptRefresh(request, options.excludedPaths)) {
      return response;
    }

    const retried = request.headers.get("X-Aquarius-Retried") === "true";
    if (retried || request.bodyUsed) {
      return response;
    }

    try {
      const tokens = await options.refreshAccessToken();
      if (!tokens?.accessToken) {
        await expireAuthentication(options.tokenStore, options.onAuthExpired, {
          cause: "refresh-unavailable",
          response,
        });
        return response;
      }

      await setTokenStoreTokens(options.tokenStore, tokens);

      return options.fetchImpl(cloneRequestForRetry(request, tokens.accessToken));
    } catch (error) {
      await expireAuthentication(options.tokenStore, options.onAuthExpired, {
        cause: "refresh-failed",
        response,
        error,
      });
      return response;
    }
  };
}

function cloneRequestForRetry(request: Request, accessToken: string): Request {
  const headers = new Headers(request.headers);
  headers.set("Authorization", `Bearer ${accessToken}`);
  headers.set("X-Aquarius-Retried", "true");

  return new Request(request, {
    headers,
  });
}

function createDefaultRefreshAccessToken(
  baseUrl: string,
  fetchImpl: typeof fetch,
  tokenStore: AuthTokenStore,
): RefreshAccessToken {
  return async () => {
    const refreshToken = await tokenStore.getRefreshToken();
    if (!refreshToken) {
      return null;
    }

    const response = await fetchImpl(
      new Request(new URL(REFRESH_TOKEN_PATH, normalizedBaseUrl(baseUrl)), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken }),
      }),
    );

    if (!response.ok) {
      return null;
    }

    return readRefreshTokens(await response.json());
  };
}

function readRefreshTokens(body: unknown): AuthTokens | null {
  const responseBody = body as RefreshTokenResponseBody;
  const accessToken = responseBody.data?.accessToken;

  if (!accessToken) {
    return null;
  }

  return {
    accessToken,
    refreshToken: responseBody.data?.refreshToken,
    accessTokenExpiresAt: expiresAt(responseBody.data?.expiresIn),
    refreshTokenExpiresAt: expiresAt(responseBody.data?.refreshExpiresIn),
  };
}

async function setTokenStoreTokens(tokenStore: AuthTokenStore, tokens: AuthTokens): Promise<void> {
  // Expiration timestamps are recorded for UI state and future proactive refresh.
  // The server remains authoritative, so 401 handling still performs refresh and cleanup.
  if (tokenStore.setTokenState) {
    await tokenStore.setTokenState({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      accessTokenExpiresAt: tokens.accessTokenExpiresAt,
      refreshTokenExpiresAt: tokens.refreshTokenExpiresAt,
    });
    return;
  }

  await tokenStore.setAccessToken(tokens.accessToken);
  if (tokens.refreshToken !== undefined) {
    await tokenStore.setRefreshToken(tokens.refreshToken);
  }
}

function expiresAt(expiresInSeconds: number | undefined): number | undefined {
  return typeof expiresInSeconds === "number" ? tokenExpiresAt(expiresInSeconds) : undefined;
}

function normalizedBaseUrl(baseUrl: string): string {
  return baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;
}

async function expireAuthentication(
  tokenStore: AuthTokenStore,
  onAuthExpired: CreateApiClientOptions["onAuthExpired"],
  reason: AuthExpiredReason,
): Promise<void> {
  await tokenStore.clearTokens();
  await onAuthExpired?.(reason);
}
