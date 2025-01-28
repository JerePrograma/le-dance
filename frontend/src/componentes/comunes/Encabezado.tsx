// Encabezado.tsx
import { useState } from "react";
import { NavLink } from "react-router-dom";
import BotonCerrarSesion from "./BotonCerrarSesion";

function Encabezado() {
  const [menuAbierto, setMenuAbierto] = useState(false);

  const alternarMenu = () => {
    setMenuAbierto(!menuAbierto);
  };

  return (
    <>
      {/* Barra superior */}
      <header className="header">
        <NavLink
          to="/"
          className="header-logo"
          onClick={() => setMenuAbierto(false)}
        >
          LE DANCE
        </NavLink>
        <button
          onClick={alternarMenu}
          className="header-menu-btn"
          aria-label="Abrir menú"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-6 w-6"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M4 6h16M4 12h16m-7 6h7"
            />
          </svg>
        </button>
      </header>

      {/* Barra lateral */}
      <aside className={`sidebar ${menuAbierto ? "sidebar-open" : ""}`}>
        <nav className="sidebar-nav">
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
          <BotonCerrarSesion />
        </nav>
      </aside>

      {/* Overlay */}
      {menuAbierto && (
        <div className="overlay lg:hidden" onClick={alternarMenu} />
      )}
    </>
  );
}

export default Encabezado;
