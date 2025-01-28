import axios from "axios";

const api = axios.create({
  baseURL: "https://jereprograma.com", // Cambiado a la URL del backend en producción
  headers: {
    "Content-Type": "application/json",
  },
});

// Interceptor para inyectar el accessToken
api.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem("accessToken");
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Interceptor de respuesta para manejar 401 (token expirado)
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) {
          throw new Error("No refreshToken en localStorage");
        }

        // Solicita un nuevo token de acceso con el refreshToken
        const { data } = await axios.post(
          "https://jereprograma.com/api/login/refresh",
          {}, // Enviar un body vacío si se envía el refreshToken en el encabezado
          {
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${refreshToken}`,
            },
          }
        );

        // Guarda los nuevos tokens en localStorage
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);

        // Reintenta la solicitud original con el nuevo token de acceso
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        console.error("Error al refrescar token:", refreshError);
        localStorage.clear();
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export default api;
