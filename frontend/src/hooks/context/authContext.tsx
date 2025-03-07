"use client";

import React, { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosConfig";
import { toast } from "react-toastify";

interface AuthContextProps {
  isAuth: boolean;
  loading: boolean;
  login: (nombreUsuario: string, contrasena: string) => Promise<void>;
  logout: () => void;
  accessToken: string | null;
  refreshToken: string | null;
  rol: string | null;
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

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isAuth, setIsAuth] = useState(false);
  const [loading, setLoading] = useState(true);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [rol, setRol] = useState<string | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    const storedAccess = localStorage.getItem("accessToken");
    const storedRefresh = localStorage.getItem("refreshToken");

    if (storedAccess && storedRefresh) {
      setAccessToken(storedAccess);
      setRefreshToken(storedRefresh);
      setIsAuth(true);
      // Obtener el perfil para establecer el rol
      api.get("/usuarios/perfil")
        .then((response) => {
          setRol(response.data.rol);
        })
        .catch((error) => {
          toast.error("Error al obtener el perfil:", error);
        });
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
    const { data } = await api.post("/login", { nombreUsuario, contrasena });

    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);

    setAccessToken(data.accessToken);
    setRefreshToken(data.refreshToken);
    setIsAuth(true);

    try {
      const profileResponse = await api.get("/usuarios/perfil");
      setRol(profileResponse.data.rol);
    } catch (error) {
      toast.error("Error al obtener el perfil:");
    }
  };

  const logout = (): void => {
    localStorage.clear();
    setAccessToken(null);
    setRefreshToken(null);
    setIsAuth(false);
    setRol(null);
    navigate("/login");
  };

  const hasRole = (role: string): boolean => {
    return rol !== null && rol === role;
  };

  return (
    <AuthContext.Provider
      value={{ isAuth, loading, login, logout, accessToken, refreshToken, rol, hasRole }}
    >
      {children}
    </AuthContext.Provider>
  );
};
