// axiosConfig.ts
import axios from "axios";
import { toast } from "react-toastify";
import { API_BASE_URL } from "../config/environment";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Interceptor para incluir el token de acceso en cada petición.
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Interceptor para manejo de errores y refrescar el token.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    // Si se recibe un 403, se redirige al login.
    if (error.response?.status === 403) {
      toast.warn("Token inválido o expirado. Redirigiendo al login...");
      localStorage.clear();
      window.location.href = "/login";
      return Promise.reject(error);
    }
    // Si se recibe un 401, se intenta refrescar el token.
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) throw new Error("No hay refreshToken almacenado.");
        const { data } = await axios.post(
          `${API_BASE_URL}/login/refresh`,
          {},
          {
            headers: {
              Authorization: `Bearer ${refreshToken}`,
              "Content-Type": "application/json",
            },
          }
        );
        // Actualizar tokens en localStorage.
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        if (data.usuario) {
          localStorage.setItem("usuario", JSON.stringify(data.usuario));
        }
        // Reintentar la petición original.
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        toast.error("Error al refrescar el token.");
        localStorage.clear();
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default api;
