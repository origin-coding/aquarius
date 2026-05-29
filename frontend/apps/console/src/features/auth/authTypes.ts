import type { components } from "@aquarius/api-types/generated/schema";

export type AuthStatus = "unknown" | "anonymous" | "authenticated";
export type LoginSuccessResponse = components["schemas"]["LoginSuccessResponse"];
export type CurrentUser = LoginSuccessResponse["user"];

