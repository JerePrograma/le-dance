// src/componentes/BarraLateral.tsx
import { useMemo } from "react";
import { NavLink } from "react-router-dom";
import styles from "./BarraLateral.module.css";
import useToggle from "./../../hooks/context/useToggle";

const BarraLateral = () => {
  const [menuAbierto, alternarMenu] = useToggle(false);

  // Lista de enlaces memorizada (la lista es fija)
  const enlaces = useMemo(
    () => [
      { to: "/", label: "Inicio" },
      { to: "/alumnos", label: "Alumnos" },
      { to: "/disciplinas", label: "Disciplinas" },
      { to: "/pagos", label: "Pagos" },
      { to: "/asistencias", label: "Asistencias" },
      { to: "/reportes", label: "Reportes" },
    ],
    []
  );

  return (
    <>
      {/* Botón de hamburguesa para móviles */}
      <button
        onClick={alternarMenu}
        className={`${styles.hamburguesa} lg:hidden`}
        aria-label="Abrir menú"
        aria-expanded={menuAbierto}
        aria-controls="menu-lateral"
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
        id="menu-lateral"
        className={`${styles.barraLateral} ${
          menuAbierto ? styles.barraAbierta : ""
        } lg:relative`}
      >
        <h1 className={styles.logo}>LE DANCE</h1>
        <nav role="menu" className={styles.navegacion}>
          {enlaces.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `${styles.enlace} ${isActive ? styles.enlaceActivo : ""}`
              }
              onClick={alternarMenu}
              role="menuitem"
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
