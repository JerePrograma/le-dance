// authContext.tsx
import React, {
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import api, { clearAuthStorage } from "../../api/axiosConfig";
import { toast } from "react-toastify";
import { AuthContext, type UserProfile } from "./auth-context";

export const AuthProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [isAuth, setIsAuth] = useState(false);
  const [loading, setLoading] = useState(true);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [user, setUser] = useState<UserProfile | null>(null);

  const navigate = useNavigate();

  // Al montar, intentamos cargar los tokens y el perfil del usuario desde localStorage
  useEffect(() => {
    const storedAccess = localStorage.getItem("accessToken");
    const storedRefresh = localStorage.getItem("refreshToken");
    const storedUser = localStorage.getItem("usuario");

    if (storedAccess && storedRefresh) {
      setAccessToken(storedAccess);
      setRefreshToken(storedRefresh);
      setIsAuth(true);

      if (storedUser) {
        setUser(JSON.parse(storedUser));
        setLoading(false);
      } else {
        // Si no se encuentra el perfil en localStorage, lo solicitamos
        api
          .get("/usuarios/perfil")
          .then((response) => {
            setUser(response.data);
          })
          .catch(() => {
            toast.error("Error al obtener el perfil");
          })
          .finally(() => {
            setLoading(false);
          });
      }
    } else {
      setLoading(false);
    }
  }, []);

  // Redirigir a login si no está autenticado (excepto en rutas públicas)
  useEffect(() => {
    if (loading) return;
    const publicPaths = ["/login"];
    if (!isAuth && !publicPaths.includes(window.location.pathname)) {
      navigate("/login");
    }
  }, [loading, isAuth, navigate]);

  const login = async (
    nombreUsuario: string,
    contrasena: string
  ): Promise<void> => {
    try {
      const { data } = await api.post("/login", { nombreUsuario, contrasena });
      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);
      localStorage.setItem("usuario", JSON.stringify(data.usuario));

      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      setIsAuth(true);
      setUser(data.usuario);
    } catch (error) {
      toast.error("Error al iniciar sesión");
      throw error;
    }
  };

  const logout = (): void => {
    clearAuthStorage();
    setAccessToken(null);
    setRefreshToken(null);
    setIsAuth(false);
    setUser(null);
    navigate("/login");
  };

  // Comparación insensible a mayúsculas para robustez
  const hasRole = (role: string): boolean => {
    return (
      user !== null &&
      user.rol.trim().toUpperCase() === role.trim().toUpperCase()
    );
  };

  return (
    <AuthContext.Provider
      value={{
        isAuth,
        loading,
        login,
        logout,
        accessToken,
        refreshToken,
        user,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
