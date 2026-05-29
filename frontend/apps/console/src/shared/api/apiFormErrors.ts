import { getApiErrorBody, type ApiErrorBody, type IssueBody } from "@aquarius/api-client";
import type { FormInstance } from "antd";

import { translateApiErrorBody, translateIssue } from "@/shared/api/apiErrorMessages";

type FieldName = string | number | (string | number)[];

type ApplyApiFormErrorsOptions = {
  fallbackCode?: string;
  fieldMap?: Record<string, FieldName>;
  form: FormInstance;
};

type ApplyApiFormErrorsResult = {
  body: ApiErrorBody | null;
  fieldMessages: Map<string, string[]>;
  message: string;
  nonFieldMessages: string[];
};

export function applyApiFormErrors(
  error: unknown,
  options: ApplyApiFormErrorsOptions,
): ApplyApiFormErrorsResult {
  const fallbackCode = options.fallbackCode ?? "common.unknown";
  const body = getApiErrorBody(error);
  const fieldMessages = collectFieldMessages(body?.issues, options.fieldMap);
  const nonFieldMessages =
    body?.issues?.filter((issue) => !issue.field).map((issue) => translateIssue(issue)) ?? [];

  if (fieldMessages.size > 0) {
    options.form.setFields(
      Array.from(fieldMessages.entries()).map(([field, errors]) => ({
        errors,
        name: fieldName(field, options.fieldMap),
      })),
    );
  }

  return {
    body,
    fieldMessages,
    message: nonFieldMessages[0] ?? translateApiErrorBody(body, fallbackCode),
    nonFieldMessages,
  };
}

export function clearApiFormErrors(form: FormInstance, fields: FieldName[]): void {
  form.setFields(
    fields.map((name) => ({
      errors: [],
      name,
    })),
  );
}

function collectFieldMessages(
  issues: IssueBody[] | undefined,
  fieldMap: ApplyApiFormErrorsOptions["fieldMap"],
): Map<string, string[]> {
  const messages = new Map<string, string[]>();

  issues
    ?.filter((issue) => issue.field)
    .forEach((issue) => {
      const field = issue.field!;
      const normalizedField = fieldKey(fieldName(field, fieldMap));
      const fieldErrors = messages.get(normalizedField) ?? [];

      fieldErrors.push(translateIssue(issue));
      messages.set(normalizedField, fieldErrors);
    });

  return messages;
}

function fieldName(field: string, fieldMap: ApplyApiFormErrorsOptions["fieldMap"]): FieldName {
  return fieldMap?.[field] ?? field;
}

function fieldKey(field: FieldName): string {
  return Array.isArray(field) ? field.join(".") : String(field);
}
