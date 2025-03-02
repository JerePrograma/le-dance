// src/componentes/comunes/Encabezado.tsx
import { useState } from "react";
import { NavLink } from "react-router-dom";
import BotonCerrarSesion from "./BotonCerrarSesion";
import { Menu, X } from "lucide-react";
import { navigationItems, NavigationItem } from "../../config/navigation";

const Encabezado: React.FC = () => {
  const [menuAbierto, setMenuAbierto] = useState(false);

  const alternarMenu = () => {
    setMenuAbierto((prev) => !prev);
  };

  return (
    <>
      {/* Header fixed */}
      <header className="bg-neutral-900 text-white border-b border-neutral-700 fixed top-0 left-0 right-0 z-50">
        <div className="container mx-auto px-4 py-2 flex justify-between items-center">
          {/* Logo / Título */}
          <NavLink
            to="/"
            className="text-2xl font-bold text-primary"
            onClick={() => setMenuAbierto(false)}
          >
            LE DANCE
          </NavLink>

          {/* Botón para abrir/cerrar menú en modo responsive */}
          <button
            onClick={alternarMenu}
            className="p-2 rounded-md bg-primary text-primary-foreground"
            aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
          >
            {menuAbierto ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
      </header>

      {/* Sidebar (menú lateral) */}
      <aside
        className={`fixed inset-y-0 right-0 z-40 w-64 bg-neutral-900 text-white shadow-lg transform
    ${menuAbierto ? "translate-x-0" : "translate-x-full"}
    transition-transform duration-300 ease-in-out`}
      >
        <nav className="h-full flex flex-col p-4">
          {navigationItems.map((item) => (
            <MenuItem
              key={item.id}
              item={item}
              onClickLink={() => setMenuAbierto(false)}
            />
          ))}

          <div className="mt-auto">
            <BotonCerrarSesion />
          </div>
        </nav>
      </aside>

      {/* Overlay gris cuando el menú está abierto */}
      {menuAbierto && (
        <div
          className="fixed inset-0 bg-black/50 z-30"
          onClick={alternarMenu}
        />
      )}
    </>
  );
};

export default Encabezado;

/**
 * Componente recursivo:
 * - Si el item NO tiene sub-items, se dibuja un <NavLink> directo.
 * - Si el item tiene .items, se dibuja un "botón" para expandir/cerrar su submenú.
 */
function MenuItem({
  item,
  onClickLink,
}: {
  item: NavigationItem;
  onClickLink: () => void;
}) {
  const [open, setOpen] = useState(false);
  const hasChildren = item.items && item.items.length > 0;

  if (!hasChildren) {
    // Item suelto => link directo
    return (
      <NavLink
        to={item.href ?? "#"}
        className={({ isActive }) =>
          `block py-2 px-4 rounded-md transition-colors ${isActive
            ? "bg-primary text-white"
            : "hover:bg-accent hover:text-accent-foreground"
          }`
        }
        onClick={onClickLink}
      >
        {item.label}
      </NavLink>
    );
  }

  // Caso: categoría con subitems
  return (
    <div>
      <button
        className="block w-full text-left py-2 px-4 rounded-md 
                   hover:bg-accent hover:text-accent-foreground"
        onClick={() => setOpen(!open)}
      >
        {item.label}
      </button>

      {open && (
        <div className="ml-4">
          {item.items!.map((sub) => (
            <NavLink
              key={sub.id}
              to={sub.href ?? "#"}
              className={({ isActive }) =>
                `block py-2 px-2 rounded-md transition-colors ${isActive
                  ? "bg-primary text-white"
                  : "hover:bg-accent hover:text-accent-foreground"
                }`
              }
              onClick={onClickLink}
            >
              {sub.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  );
}
