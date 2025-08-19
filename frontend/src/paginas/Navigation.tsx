// Navigation.tsx
import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../hooks/context/authContext";
import { NavigationItem, navigationItems } from "../config/navigation";

const NavigationMenu: React.FC = () => {
  const { hasRole, user, loading } = useAuth();

  // Mientras se carga el perfil o no está definido, mostramos un fallback.
  if (loading || !user) {
    return <div>Cargando navegación...</div>;
  }

  // Función recursiva para filtrar ítems según requiredRole.
  const filterNavigationItems = (items: NavigationItem[]): NavigationItem[] => {
    return items
      .filter((item) => {
        if (item.requiredRole) {
          const allowed = hasRole(item.requiredRole);
          return allowed;
        }
        return true;
      })
      .map((item) => ({
        ...item,
        items: item.items ? filterNavigationItems(item.items) : undefined,
      }));
  };

  const filteredItems = filterNavigationItems(navigationItems);
  console.log("Ítems de navegación filtrados:", filteredItems);

  return (
    <nav>
      <ul>
        {filteredItems.map((item) => (
          <li key={item.id} className="mb-2">
            {item.href ? (
              <Link to={item.href} className="font-semibold hover:underline">
                {item.label}
              </Link>
            ) : (
              <span className="font-semibold">{item.label}</span>
            )}
            {item.description && (
              <small className="block text-xs text-gray-500">
                {item.description}
              </small>
            )}
            {item.items && item.items.length > 0 && (
              <ul className="ml-4 mt-1">
                {item.items.map((subItem) => (
                  <li key={subItem.id} className="mb-1">
                    {subItem.href ? (
                      <Link to={subItem.href} className="hover:underline">
                        {subItem.label}
                      </Link>
                    ) : (
                      <span>{subItem.label}</span>
                    )}
                    {subItem.description && (
                      <small className="block text-xs text-gray-500">
                        {subItem.description}
                      </small>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </nav>
  );
};

export default NavigationMenu; 