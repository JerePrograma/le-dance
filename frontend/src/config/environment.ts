const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

if (!configuredApiBaseUrl && import.meta.env.PROD) {
  throw new Error("VITE_API_BASE_URL es obligatoria en producción");
}

const apiUrl = new URL(configuredApiBaseUrl || "http://localhost:8080/api");
const isLocal = ["localhost", "127.0.0.1"].includes(apiUrl.hostname);

if (import.meta.env.PROD && apiUrl.protocol !== "https:" && !isLocal) {
  throw new Error("VITE_API_BASE_URL debe usar HTTPS en producción");
}

export const API_BASE_URL = apiUrl.toString().replace(/\/$/, "");
export const APP_TIME_ZONE =
  import.meta.env.VITE_APP_TIME_ZONE?.trim() ||
  "America/Argentina/Buenos_Aires";
