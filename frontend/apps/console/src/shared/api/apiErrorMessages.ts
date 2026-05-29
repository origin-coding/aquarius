import { getApiErrorBody, type ApiErrorBody, type IssueBody } from "@aquarius/api-client";
import i18n from "i18next";

type TranslationArgs = Record<string, string>;
type DynamicTranslationOptions = TranslationArgs & {
  defaultValue?: string;
  ns: "errors";
};
type DynamicTranslator = (key: string, options: DynamicTranslationOptions) => string;

const translateDynamic = i18n.t.bind(i18n) as DynamicTranslator;

export function translateApiError(error: unknown, fallbackCode = "common.unknown"): string {
  const body = getApiErrorBody(error);
  const code = body?.code ?? fallbackCode;

  return translateErrorCode(code, messageArguments(body), fallbackCode);
}

export function translateApiErrorBody(
  body: ApiErrorBody | null | undefined,
  fallbackCode = "common.unknown",
): string {
  const code = body?.code ?? fallbackCode;

  return translateErrorCode(code, messageArguments(body), fallbackCode);
}

export function translateIssue(issue: IssueBody, fallbackCode = "validation.invalid"): string {
  return translateErrorCode(issue.code ?? fallbackCode, messageArguments(issue), fallbackCode);
}

export function translateIssues(issues: IssueBody[] | undefined): string[] {
  return issues?.map((issue) => translateIssue(issue)) ?? [];
}

function translateErrorCode(code: string, args: TranslationArgs, fallbackCode: string): string {
  return translateDynamic(code, {
    ...args,
    defaultValue: translateDynamic(fallbackCode, {
      defaultValue: code,
      ns: "errors",
    }),
    ns: "errors",
  });
}

function messageArguments(
  body: Pick<ApiErrorBody, "arguments"> | Pick<IssueBody, "arguments"> | null | undefined,
): TranslationArgs {
  return Object.fromEntries(
    body?.arguments
      ?.filter((argument) => argument.name && argument.value !== undefined)
      .map((argument) => [argument.name, String(argument.value)]) ?? [],
  );
}
