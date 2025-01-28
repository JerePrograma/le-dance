import { useState } from "react";
import { NavLink } from "react-router-dom";
import styles from "./BarraLateral.module.css";

const BarraLateral = () => {
  const [menuAbierto, setMenuAbierto] = useState(false);

  const alternarMenu = () => {
    setMenuAbierto((prev) => !prev);
  };

  return (
    <>
      {/* Botón de hamburguesa para móviles */}
      <button
        onClick={alternarMenu}
        className={`${styles.hamburguesa} lg:hidden`}
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

      {/* Barra lateral */}
      <aside
        className={`${styles.barraLateral} ${
          menuAbierto ? styles.barraAbierta : ""
        } lg:relative`}
      >
        <h1 className={styles.logo}>LE DANCE</h1>
        <nav className={styles.navegacion}>
          {[
            { to: "/", label: "Inicio" },
            { to: "/alumnos", label: "Alumnos" },
            { to: "/disciplinas", label: "Disciplinas" },
            { to: "/pagos", label: "Pagos" },
            { to: "/asistencias", label: "Asistencias" },
            { to: "/reportes", label: "Reportes" },
          ].map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `${styles.enlace} ${isActive ? styles.enlaceActivo : ""}`
              }
              onClick={alternarMenu}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Fondo oscuro para cerrar el menú en móviles */}
      {menuAbierto && (
        <div
          onClick={alternarMenu}
          className={styles.fondoOscuro}
          aria-hidden="true"
        ></div>
      )}
    </>
  );
};

export default BarraLateral;
