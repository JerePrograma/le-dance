// axiosConfig.ts
import axios from "axios";
import { toast } from "react-toastify";

const isDevelopment = process.env.NODE_ENV === "development";
const baseURL = isDevelopment
  ? "http://localhost:8080/api"
  : "http://82.25.68.219/api";

const api = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 403) {
      toast.warn("Token inválido o expirado. Redirigiendo al login...");
      localStorage.clear();
      window.location.href = "/login";
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) throw new Error("No hay refreshToken almacenado.");

        const { data } = await axios.post(
          `${baseURL}/login/refresh`,
          {},
          {
            headers: {
              Authorization: `Bearer ${refreshToken}`,
              "Content-Type": "application/json",
            },
          }
        );

        // Actualizar localStorage con nuevos tokens y perfil de usuario
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        localStorage.setItem("usuario", JSON.stringify(data.usuario));

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
