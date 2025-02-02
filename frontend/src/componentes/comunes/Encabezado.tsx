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
      <header className="bg-background dark:bg-background-dark border-b border-border dark:border-border-dark fixed top-0 left-0 right-0 z-50">
        <div className="@container mx-auto px-4 py-2 flex justify-between items-center">
          <NavLink
            to="/"
            className="text-2xl font-bold text-primary dark:text-primary-light"
            onClick={() => setMenuAbierto(false)}
          >
            LE DANCE
          </NavLink>
          <button
            onClick={alternarMenu}
            className="@container:hidden p-2 rounded-md bg-primary text-primary-foreground dark:bg-primary-light dark:text-background-dark hover:bg-primary/90 dark:hover:bg-primary-light/90 transition-colors"
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
        className={`fixed inset-y-0 right-0 z-40 w-64 bg-background dark:bg-background-dark shadow-lg transform
                    ${menuAbierto ? "translate-x-0" : "translate-x-full"}
                    transition-transform duration-300 ease-[var(--ease-in-out)]
                    @container:translate-x-0 @container:static @container:w-auto`}
      >
        <nav className="h-full flex flex-col p-4">
          {[
            { to: "/", label: "Inicio" },
            { to: "/profesores", label: "Profesores" },
            { to: "/disciplinas", label: "Disciplinas" },
            { to: "/alumnos", label: "Alumnos" },
            { to: "/asistencias", label: "Asistencias" },
            { to: "/bonificaciones", label: "Bonificaciones" },
            { to: "/pagos", label: "Pagos" },
            { to: "/reportes", label: "Reportes" },
          ].map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `block py-2 px-4 rounded-md transition-colors
                 ${
                   isActive
                     ? "bg-primary text-primary-foreground dark:bg-primary-light dark:text-background-dark"
                     : "text-foreground dark:text-text-dark hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent-dark dark:hover:text-accent-foreground-dark"
                 }`
              }
              onClick={() => setMenuAbierto(false)}
            >
              {link.label}
            </NavLink>
          ))}
          <div className="mt-auto">
            <BotonCerrarSesion />
          </div>
        </nav>
      </aside>

      {menuAbierto && (
        <div
          className="fixed inset-0 bg-black/50 z-30 @container:hidden"
          onClick={alternarMenu}
        />
      )}
    </>
  );
}

export default Encabezado;
