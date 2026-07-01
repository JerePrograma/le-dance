import { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { describe, expect, it } from "vitest";
import { errorCategory, getApiError, type ApiErrorPayload } from "./apiError";

const failure = (status: number, code: string) => new AxiosError(
  "request failed",
  undefined,
  {} as InternalAxiosRequestConfig,
  undefined,
  { status, statusText: "error", headers: {}, config: {} as InternalAxiosRequestConfig,
    data: { timestamp: "2026-07-01T00:00:00Z", status, code, message: "error", fieldErrors: [] } satisfies ApiErrorPayload },
);

describe("apiError", () => {
  it("preserva código y distingue las categorías HTTP del contrato", () => {
    expect(getApiError(failure(409, "IDEMPOTENCY_CONFLICT"))?.code).toBe("IDEMPOTENCY_CONFLICT");
    expect([400, 409, 401, 403, 404, 500].map((status) => errorCategory(failure(status, "X"))))
      .toEqual(["validation", "conflict", "unauthorized", "forbidden", "not-found", "internal"]);
    expect(errorCategory(new Error("offline"))).toBe("unknown");
  });
});
