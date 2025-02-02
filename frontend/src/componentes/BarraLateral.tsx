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
        className="@container:hidden fixed top-4 left-4 z-50 p-2 rounded-[var(--radius-md)] 
                  bg-[#8B5CF6] text-white hover:bg-[#7C3AED] 
                  transition-colors duration-200 ease-[var(--ease-out)]"
        aria-label="Abrir menú"
        aria-expanded={menuAbierto}
        aria-controls="menu-lateral"
      >
        <Menu className="h-6 w-6" />
      </button>

      <aside
        id="menu-lateral"
        className={`fixed inset-y-0 left-0 z-40 w-64 
                    bg-[#F9FAFB] border-r border-gray-200
                    transform ${
                      menuAbierto ? "translate-x-0" : "-translate-x-full"
                    }
                    transition-transform duration-300 ease-[var(--ease-in-out)]
                    @container:translate-x-0 @container:static @container:w-auto`}
      >
        <h1 className="text-2xl font-bold text-[#8B5CF6] p-4">LE DANCE</h1>
        <nav role="menu" className="space-y-2 p-4">
          {enlaces.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `block py-2 px-4 rounded-[var(--radius-md)] transition-colors
                 ${
                   isActive
                     ? "bg-[#8B5CF6] text-white"
                     : "text-[#1F2937] hover:bg-[#8B5CF6]/10"
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
          className="fixed inset-0 bg-black/50 z-30 @container:hidden"
          aria-hidden="true"
        ></div>
      )}
    </>
  );
};

export default BarraLateral;
