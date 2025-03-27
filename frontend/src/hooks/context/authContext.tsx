// authContext.tsx
import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosConfig";
import { toast } from "react-toastify";

// Definimos la interfaz del perfil de usuario
export interface UserProfile {
  id: number;
  nombre: string;
  email?: string;
  rol: string;
  // Otros campos que devuelva tu endpoint de perfil
}

interface AuthContextProps {
  isAuth: boolean;
  loading: boolean;
  login: (nombreUsuario: string, contrasena: string) => Promise<void>;
  logout: () => void;
  accessToken: string | null;
  refreshToken: string | null;
  user: UserProfile | null;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextProps | undefined>(undefined);

export const useAuth = (): AuthContextProps => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [isAuth, setIsAuth] = useState(false);
  const [loading, setLoading] = useState(true);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [user, setUser] = useState<UserProfile | null>(null);

  const navigate = useNavigate();

  // Al montar, intentamos cargar los tokens y obtener el perfil del usuario
  useEffect(() => {
    const storedAccess = localStorage.getItem("accessToken");
    const storedRefresh = localStorage.getItem("refreshToken");

    if (storedAccess && storedRefresh) {
      setAccessToken(storedAccess);
      setRefreshToken(storedRefresh);
      setIsAuth(true);
      // Obtener el perfil completo del usuario
      api
        .get("/usuarios/perfil")
        .then((response) => {
          setUser(response.data);
        })
        .catch((error) => {
          toast.error("Error al obtener el perfil", error);
        })
        .finally(() => {
          // Ahora solo marcamos loading en false al finalizar la petición
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, []);

  // Si no está autenticado, redirige a login (excepto en rutas públicas)
  useEffect(() => {
    if (loading) return;
    const publicPaths = ["/login", "/registro"];
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

      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      setIsAuth(true);

      // Obtener perfil completo del usuario luego del login
      const profileResponse = await api.get("/usuarios/perfil");
      setUser(profileResponse.data);
    } catch (error) {
      toast.error("Error al iniciar sesión");
      throw error;
    }
  };

  const logout = (): void => {
    localStorage.clear();
    setAccessToken(null);
    setRefreshToken(null);
    setIsAuth(false);
    setUser(null);
    navigate("/login");
  };

  // Comparación insensible a mayúsculas para mayor robustez
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
