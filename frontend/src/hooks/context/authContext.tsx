import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosConfig";

interface AuthContextProps {
  isAuth: boolean;
  loading: boolean;
  login: (nombreUsuario: string, contrasena: string) => Promise<void>;
  logout: () => void;
  accessToken: string | null;
  refreshToken: string | null;
}

const AuthContext = createContext<AuthContextProps | undefined>(undefined);

export const useAuth = (): AuthContextProps => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe estar dentro de AuthProvider");
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

  const navigate = useNavigate();

  useEffect(() => {
    const storedAccess = localStorage.getItem("accessToken");
    const storedRefresh = localStorage.getItem("refreshToken");

    if (storedAccess && storedRefresh) {
      setAccessToken(storedAccess);
      setRefreshToken(storedRefresh);
      setIsAuth(true);
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    if (loading) return;

    const publicPaths = ["/login", "/registro"];
    if (!isAuth && !publicPaths.includes(window.location.pathname)) {
      navigate("/login");
    }
  }, [loading, isAuth, navigate]);

  const login = async (nombreUsuario: string, contrasena: string): Promise<void> => {
    const { data } = await api.post("/api/login", { nombreUsuario, contrasena });

    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);

    setAccessToken(data.accessToken);
    setRefreshToken(data.refreshToken);
    setIsAuth(true);
  };

  const logout = (): void => {
    localStorage.clear();
    setAccessToken(null);
    setRefreshToken(null);
    setIsAuth(false);
    navigate("/login");
  };

  return (
    <AuthContext.Provider
      value={{ isAuth, loading, login, logout, accessToken, refreshToken }}
    >
      {children}
    </AuthContext.Provider>
  );
};
