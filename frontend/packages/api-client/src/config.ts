export const DEFAULT_API_BASE_URL = "http://localhost:8080";

export type ApiClientConfig = {
  baseUrl?: string;
};

export function getApiBaseUrl(config: ApiClientConfig = {}): string {
  return config.baseUrl ?? DEFAULT_API_BASE_URL;
}
