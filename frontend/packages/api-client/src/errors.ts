import type { components } from "@aquarius/api-types/generated/schema";

export type MessageArgument = components["schemas"]["MessageArgument"];
export type IssueBody = components["schemas"]["IssueBody"];

export type ApiErrorBody = {
  code?: string;
  arguments?: MessageArgument[];
  issues?: IssueBody[];
  warnings?: IssueBody[];
  timestamp?: string;
  requestId?: string;
};

export function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return typeof value === "object" && value !== null && "code" in value;
}

export function getApiErrorCode(value: unknown): string | null {
  return isApiErrorBody(value) && typeof value.code === "string" ? value.code : null;
}
