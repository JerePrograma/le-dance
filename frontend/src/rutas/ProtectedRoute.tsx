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
    return <div>Cargando...</div>;
  }

  if (!isAuth) {
    return <Navigate to={redirectPath} replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    // Redirige a una ruta de "No autorizado" si no se cumple el rol
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
