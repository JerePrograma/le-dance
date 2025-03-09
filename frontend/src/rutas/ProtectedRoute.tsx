import React from "react";
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/context/authContext";

interface ProtectedRouteProps {
  redirectPath?: string;
  requiredRole?: string;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  redirectPath = "/login",
  requiredRole,
}) => {
  const { isAuth, loading, hasRole } = useAuth();

  if (loading) {
    // Se podría reemplazar por un componente de carga personalizado
    return <div>Cargando...</div>;
  }

  if (!isAuth) {
    return <Navigate to={redirectPath} replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    // Redirige a la ruta "No autorizado" si el usuario no cumple el rol requerido
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
