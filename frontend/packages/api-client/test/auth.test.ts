import { describe, expect, it, vi } from "vitest";

import { createMemoryAuthTokenStore, createSingleFlightRefresh, getApiErrorBody } from "../src";

describe("memory auth token store", () => {
  it("gets, sets, and clears tokens", async () => {
    const store = createMemoryAuthTokenStore({
      accessToken: "access-1",
      refreshToken: "refresh-1",
    });

    expect(await store.getAccessToken()).toBe("access-1");
    expect(await store.getRefreshToken()).toBe("refresh-1");
    expect(await store.getTokenState?.()).toEqual({
      accessToken: "access-1",
      refreshToken: "refresh-1",
      accessTokenExpiresAt: null,
      refreshTokenExpiresAt: null,
    });

    await store.setAccessToken("access-2");
    await store.setRefreshToken("refresh-2");

    expect(await store.getAccessToken()).toBe("access-2");
    expect(await store.getRefreshToken()).toBe("refresh-2");

    await store.setTokenState?.({
      accessTokenExpiresAt: 1000,
      refreshTokenExpiresAt: 2000,
    });

    expect(await store.getTokenState?.()).toEqual({
      accessToken: "access-2",
      refreshToken: "refresh-2",
      accessTokenExpiresAt: 1000,
      refreshTokenExpiresAt: 2000,
    });

    await store.clearTokens();

    expect(await store.getAccessToken()).toBeNull();
    expect(await store.getRefreshToken()).toBeNull();
  });
});

describe("single-flight refresh", () => {
  it("runs concurrent refresh requests once", async () => {
    const refresh = vi.fn(async () => {
      await new Promise((resolve) => setTimeout(resolve, 1));
      return { accessToken: "access-2", refreshToken: "refresh-2" };
    });

    const singleFlightRefresh = createSingleFlightRefresh(refresh);

    const [first, second, third] = await Promise.all([
      singleFlightRefresh(),
      singleFlightRefresh(),
      singleFlightRefresh(),
    ]);

    expect(first).toEqual({ accessToken: "access-2", refreshToken: "refresh-2" });
    expect(second).toEqual(first);
    expect(third).toEqual(first);
    expect(refresh).toHaveBeenCalledTimes(1);
  });
});

describe("API error body helpers", () => {
  it("reads structured API error bodies", () => {
    expect(
      getApiErrorBody({
        code: "iam.auth.invalid_credentials",
        arguments: [{ name: "username", value: "admin" }],
      }),
    ).toEqual({
      code: "iam.auth.invalid_credentials",
      arguments: [{ name: "username", value: "admin" }],
    });
  });

  it("treats legacy Error messages as error codes", () => {
    expect(getApiErrorBody(new Error("auth.login_failed"))).toEqual({
      code: "auth.login_failed",
    });
  });
});
