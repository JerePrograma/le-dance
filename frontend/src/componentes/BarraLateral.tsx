import { useMemo } from "react";
import { NavLink } from "react-router-dom";
import useToggle from "../hooks/useToggle";
import { Menu } from "lucide-react";

const BarraLateral = () => {
  const [menuAbierto, alternarMenu] = useToggle(false);

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
      <button
        onClick={alternarMenu}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-md bg-primary text-primary-foreground"
        aria-label="Abrir menú"
        aria-expanded={menuAbierto}
        aria-controls="menu-lateral"
      >
        <Menu className="h-6 w-6" />
      </button>

      <aside
        id="menu-lateral"
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-background shadow-lg transform ${
          menuAbierto ? "translate-x-0" : "-translate-x-full"
        } transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:w-auto`}
      >
        <h1 className="text-2xl font-bold text-primary p-4">LE DANCE</h1>
        <nav role="menu" className="space-y-2 p-4">
          {enlaces.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `block py-2 px-4 rounded-md transition-colors ${
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-foreground hover:bg-accent hover:text-accent-foreground"
                }`
              }
              onClick={alternarMenu}
              role="menuitem"
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {menuAbierto && (
        <div
          onClick={alternarMenu}
          className="fixed inset-0 bg-black bg-opacity-50 z-30 lg:hidden"
          aria-hidden="true"
        ></div>
      )}
    </>
  );
};

export default BarraLateral;
