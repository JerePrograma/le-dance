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
  const { isAuth, loading, user, hasRole } = useAuth();

  // Mientras se carga el perfil, muestra un indicador de carga
  if (loading || (isAuth && !user)) {
    return <div>Cargando perfil...</div>;
  }

  // Si no está autenticado, redirige
  if (!isAuth) {
    return <Navigate to={redirectPath} replace />;
  }

  // Si se requiere un rol y el usuario no lo tiene, redirige a no autorizado
  if (requiredRole && !hasRole(requiredRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
