import type React from "react";
import { useAuth } from "../../hooks/context/authContext";
import Boton from "./Boton";
import { LogOut } from "lucide-react";

const BotonCerrarSesion: React.FC = () => {
  const { logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <Boton
      onClick={handleLogout}
      className="w-full mt-4 border border-input bg-background hover:bg-accent hover:text-accent-foreground"
    >
      <LogOut className="w-4 h-4 mr-2" />
      Cerrar sesión
    </Boton>
  );
};

export default BotonCerrarSesion;
