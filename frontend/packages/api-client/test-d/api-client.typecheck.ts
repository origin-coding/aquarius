import { createAquariusApiClient } from "../src";

const api = createAquariusApiClient({ baseUrl: "http://api.test" });

api.GET("/iam/captchas/password-login");

api.POST("/iam/auth/sessions/password", {
  body: {
    loginName: "admin",
    password: "password",
    captchaChallengeId: "local",
    captchaCode: "8888",
  },
});

api.POST("/iam/auth/sessions/refresh-token", {
  body: {
    refreshToken: "refresh-token",
  },
});

// @ts-expect-error wrong path
api.GET("/wrong/path");

api.POST("/iam/auth/sessions/password", {
  // @ts-expect-error missing required body fields
  body: {
    loginName: "admin",
  },
});
