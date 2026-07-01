import axios from "axios";

export interface ApiErrorPayload {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  fieldErrors: Array<{ field: string; message: string }>;
}

export const getApiError = (error: unknown): ApiErrorPayload | null => {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) return null;
  const data = error.response?.data;
  return data && typeof data.status === "number" && typeof data.code === "string" ? data : null;
};

export const errorCategory = (error: unknown): "validation" | "conflict" | "unauthorized" | "forbidden" | "not-found" | "internal" | "unknown" => {
  const status = getApiError(error)?.status;
  if (status === 400) return "validation";
  if (status === 409) return "conflict";
  if (status === 401) return "unauthorized";
  if (status === 403) return "forbidden";
  if (status === 404) return "not-found";
  if (status !== undefined && status >= 500) return "internal";
  return "unknown";
};
