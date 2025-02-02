import { useState } from "react";
import { NavLink } from "react-router-dom";
import BotonCerrarSesion from "./BotonCerrarSesion";
import { Menu, X } from "lucide-react";

function Encabezado() {
  const [menuAbierto, setMenuAbierto] = useState(false);

  const alternarMenu = () => {
    setMenuAbierto(!menuAbierto);
  };

  return (
    <>
      <header className="bg-background border-b border-border fixed top-0 left-0 right-0 z-50">
        <div className="container mx-auto px-4 py-2 flex justify-between items-center">
          <NavLink
            to="/"
            className="text-2xl font-bold text-primary"
            onClick={() => setMenuAbierto(false)}
          >
            LE DANCE
          </NavLink>
          <button
            onClick={alternarMenu}
            className="lg:hidden p-2 rounded-md bg-primary text-primary-foreground"
            aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
          >
            {menuAbierto ? (
              <X className="h-6 w-6" />
            ) : (
              <Menu className="h-6 w-6" />
            )}
          </button>
        </div>
      </header>

      <aside
        className={`fixed inset-y-0 right-0 z-40 w-64 bg-background shadow-lg transform ${
          menuAbierto ? "translate-x-0" : "translate-x-full"
        } transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:w-auto`}
      >
        <nav className="h-full flex flex-col p-4">
          <NavLink
            to="/"
            className="sidebar-link"
            onClick={() => setMenuAbierto(false)}
          >
            Inicio
          </NavLink>
          <NavLink
            to="/profesores"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Profesores
          </NavLink>
          <NavLink
            to="/disciplinas"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Disciplinas
          </NavLink>
          <NavLink
            to="/alumnos"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Alumnos
          </NavLink>
          <NavLink
            to="/asistencias"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Asistencias
          </NavLink>
          <NavLink
            to="/bonificaciones"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Bonificaciones
          </NavLink>
          <NavLink to="/pagos" className="sidebar-link" onClick={alternarMenu}>
            Pagos
          </NavLink>
          <NavLink
            to="/reportes"
            className="sidebar-link"
            onClick={alternarMenu}
          >
            Reportes
          </NavLink>
          <div className="mt-auto">
            <BotonCerrarSesion />
          </div>
        </nav>
      </aside>

      {menuAbierto && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-30 lg:hidden"
          onClick={alternarMenu}
        />
      )}
    </>
  );
}

export default Encabezado;
