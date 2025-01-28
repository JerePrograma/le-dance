import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
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
        // Intentamos refrescar usando el refreshToken
        const refreshToken = localStorage.getItem("refreshToken");
        if (!refreshToken) {
          throw new Error("No refreshToken en localStorage");
        }

        // Ojo: servidor recibe el token como string en el body
        const { data } = await axios.post(
          "http://localhost:8080/api/login/refresh",
          refreshToken,
          {
            headers: { "Content-Type": "text/plain" },
          }
        );

        // data = { accessToken, refreshToken }
        localStorage.setItem("accessToken", data.accessToken);
        localStorage.setItem("refreshToken", data.refreshToken);

        // Reintentamos la request original con el nuevo accessToken
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
