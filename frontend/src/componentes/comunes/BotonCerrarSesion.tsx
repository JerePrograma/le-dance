// BotonCerrarSesion.tsx
import React from "react";
import { useAuth } from "../../hooks/context/authContext";

const BotonCerrarSesion: React.FC = () => {
  const { logout } = useAuth();

  const handleLogout = () => {
    logout(); // Llama a la función logout del AuthProvider
  };

  return <button onClick={handleLogout}>Cerrar sesión</button>;
};

export default BotonCerrarSesion;
