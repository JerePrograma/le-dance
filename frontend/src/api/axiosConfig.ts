import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios";
import { toast } from "react-toastify";
import { API_BASE_URL } from "../config/environment";
import type { UsuarioResponse } from "../types/types";

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  usuario?: UsuarioResponse;
}

export const AUTH_STORAGE_KEYS = [
  "accessToken",
  "refreshToken",
  "usuario",
] as const;

export function clearAuthStorage(): void {
  AUTH_STORAGE_KEYS.forEach((key) => localStorage.removeItem(key));
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

let refreshPromise: Promise<RefreshResponse> | null = null;

function isRefreshRequest(config: InternalAxiosRequestConfig): boolean {
  return config.url?.replace(API_BASE_URL, "").startsWith("/login/refresh") ?? false;
}

function redirectToLogin(): void {
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;
    const status = error.response?.status;

    if (status === 403) {
      toast.warn("No tenés permisos para realizar esta acción.");
      return Promise.reject(error);
    }

    if (
      status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isRefreshRequest(originalRequest)
    ) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    const refreshToken = localStorage.getItem("refreshToken");
    if (!refreshToken) {
      clearAuthStorage();
      redirectToLogin();
      return Promise.reject(error);
    }

    try {
      refreshPromise ??= axios
        .post<RefreshResponse>(
          `${API_BASE_URL}/login/refresh`,
          {},
          {
            headers: {
              Authorization: `Bearer ${refreshToken}`,
              "Content-Type": "application/json",
            },
          }
        )
        .then((response) => response.data);

      const data = await refreshPromise;
      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);
      if (data.usuario) {
        localStorage.setItem("usuario", JSON.stringify(data.usuario));
      }
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      clearAuthStorage();
      toast.error("La sesión expiró. Iniciá sesión nuevamente.");
      redirectToLogin();
      return Promise.reject(refreshError);
    } finally {
      refreshPromise = null;
    }
  }
);

export default api;
