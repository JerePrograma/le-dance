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

// Agregar interceptor para incluir el token de acceso
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Interceptor para manejo de errores y refrescar token
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    // Si recibimos un 403, el token es inválido: redirigimos al login
    if (error.response?.status === 403) {
      toast.warn("Token inválido o expirado. Redirigiendo al login...");
      localStorage.clear();
      window.location.href = "/login";
      return Promise.reject(error);
    }
    // Si recibimos 401 y no se ha reintentado el request, intentamos refrescar el token
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
        // Actualizamos tokens y perfil de usuario en localStorage
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);
        if (data.usuario) {
          localStorage.setItem("usuario", JSON.stringify(data.usuario));
        }
        // Actualizamos el header Authorization y reintentamos la petición original
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
