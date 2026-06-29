import { describe, expect, it } from "vitest";
import { resolveEnvironment } from "./environment";

describe("resolveEnvironment", () => {
  it("usa defaults locales sólo fuera de producción", () => {
    expect(
      resolveEnvironment({
        PROD: false,
        VITE_API_BASE_URL: undefined,
        VITE_APP_TIME_ZONE: undefined,
      })
    ).toEqual({
      apiBaseUrl: "http://localhost:8080/api",
      appTimeZone: "America/Argentina/Buenos_Aires",
    });
  });

  it("rechaza una producción sin URL", () => {
    expect(() =>
      resolveEnvironment({
        PROD: true,
        VITE_API_BASE_URL: undefined,
        VITE_APP_TIME_ZONE: undefined,
      })
    ).toThrow("VITE_API_BASE_URL es obligatoria en producción");
  });

  it("rechaza HTTP remoto en producción", () => {
    expect(() =>
      resolveEnvironment({
        PROD: true,
        VITE_API_BASE_URL: "http://api.example.test/api",
        VITE_APP_TIME_ZONE: "America/Argentina/Buenos_Aires",
      })
    ).toThrow("VITE_API_BASE_URL debe usar HTTPS en producción");
  });
});
