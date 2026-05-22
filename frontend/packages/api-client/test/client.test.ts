import { describe, expect, it, vi } from "vitest";
import { createMemoryAuthTokenStore, createAquariusApiClient } from "../src";

describe("Aquarius API client auth", () => {
  it("adds the access token to requests", async () => {
    const store = createMemoryAuthTokenStore({ accessToken: "access-1" });
    const fetchMock = vi.fn(async (request: RequestInfo | URL) => {
      const headers = new Headers((request as Request).headers);
      expect(headers.get("Authorization")).toBe("Bearer access-1");
      return jsonResponse({ code: "ok", data: {} });
    });

    const client = createAquariusApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokenStore: store,
    });

    await client.GET("/iam/captchas/password-login");

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not add Authorization when no access token exists", async () => {
    const fetchMock = vi.fn(async (request: RequestInfo | URL) => {
      const headers = new Headers((request as Request).headers);
      expect(headers.has("Authorization")).toBe(false);
      return jsonResponse({ code: "ok", data: {} });
    });

    const client = createAquariusApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokenStore: createMemoryAuthTokenStore(),
    });

    await client.GET("/iam/captchas/password-login");

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("refreshes once and retries concurrent 401 responses", async () => {
    const store = createMemoryAuthTokenStore({
      accessToken: "access-1",
      refreshToken: "refresh-1",
    });
    let protectedRequestCount = 0;
    const fetchMock = vi.fn(async (request: RequestInfo | URL) => {
      const url = requestUrl(request);
      const authorization = (request as Request).headers.get("Authorization");

      if (url.pathname === "/iam/auth/sessions/refresh-token") {
        expect(await (request as Request).json()).toEqual({ refreshToken: "refresh-1" });
        return jsonResponse({
          code: "ok",
          data: {
            accessToken: "access-2",
            refreshToken: "refresh-2",
            expiresIn: 900,
            refreshExpiresIn: 3600,
          },
        });
      }

      protectedRequestCount += 1;

      if (authorization === "Bearer access-1") {
        return jsonResponse({ code: "iam.auth.unauthenticated" }, 401);
      }

      expect(authorization).toBe("Bearer access-2");
      return jsonResponse({ code: "ok" });
    });

    const client = createAquariusApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokenStore: store,
    });

    const [first, second] = await Promise.all([
      client.GET("/iam/captchas/password-login"),
      client.GET("/iam/captchas/password-login"),
    ]);

    expect(first.response.status).toBe(200);
    expect(second.response.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(5);
    expect(protectedRequestCount).toBe(4);
    expect((await store.getTokenState?.())?.accessTokenExpiresAt).toEqual(expect.any(Number));
    expect((await store.getTokenState?.())?.refreshTokenExpiresAt).toEqual(expect.any(Number));
  });

  it("does not recursively refresh the refresh endpoint", async () => {
    const store = createMemoryAuthTokenStore({
      accessToken: "access-1",
      refreshToken: "refresh-1",
    });
    const onAuthExpired = vi.fn();
    const fetchMock = vi.fn(async () =>
      jsonResponse({ code: "iam.auth.invalid_refresh_token" }, 401),
    );

    const client = createAquariusApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokenStore: store,
      onAuthExpired,
    });

    await client.POST("/iam/auth/sessions/refresh-token", {
      body: { refreshToken: "refresh-1" },
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(onAuthExpired).not.toHaveBeenCalled();
  });

  it("clears tokens and calls onAuthExpired when refresh fails", async () => {
    const store = createMemoryAuthTokenStore({
      accessToken: "access-1",
      refreshToken: "refresh-1",
    });
    const onAuthExpired = vi.fn();
    const fetchMock = vi.fn(async (request: RequestInfo | URL) => {
      const url = requestUrl(request);

      if (url.pathname === "/iam/auth/sessions/refresh-token") {
        return jsonResponse({ code: "iam.auth.invalid_refresh_token" }, 401);
      }

      return jsonResponse({ code: "iam.auth.unauthenticated" }, 401);
    });

    const client = createAquariusApiClient({
      baseUrl: "http://api.test",
      fetch: fetchMock,
      tokenStore: store,
      onAuthExpired,
    });

    const result = await client.GET("/iam/captchas/password-login");

    expect(result.response.status).toBe(401);
    expect(await store.getAccessToken()).toBeNull();
    expect(await store.getRefreshToken()).toBeNull();
    expect(onAuthExpired).toHaveBeenCalledWith(
      expect.objectContaining({ cause: "refresh-unavailable" }),
    );
  });
});

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}

function requestUrl(request: RequestInfo | URL): URL {
  if (request instanceof Request) {
    return new URL(request.url);
  }

  return new URL(request);
}
