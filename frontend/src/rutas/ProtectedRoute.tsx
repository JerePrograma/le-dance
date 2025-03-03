import React from "react";
import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/context/authContext";

const ProtectedRoute: React.FC<{ redirectPath?: string }> = ({
  redirectPath = "/login",
}) => {
  const { isAuth, loading } = useAuth();

  // Mostrar un spinner o mensaje de carga mientras se verifica la autenticacion
  if (loading) {
    return <div>Cargando...</div>; // Puedes reemplazar esto por un Spinner o componente de carga
  }

  // Redirige al login si no esta autenticado
  return isAuth ? <Outlet /> : <Navigate to={redirectPath} replace />;
};

export default ProtectedRoute;
