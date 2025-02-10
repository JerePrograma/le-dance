// src/paginas/Dashboard.tsx
import { Link } from "react-router-dom";
import { navigationItems, NavigationItem } from "../config/navigation";
import React from "react";

const Dashboard: React.FC = React.memo(() => {
  return (
    <div className="dashboard-container">
      <main>
        <h2 className="text-3xl font-bold text-center mb-12">
          Panel de Gestión - LE DANCE
        </h2>
        <div className="dashboard-grid">
          {navigationItems.map(({ icon: Icon, label, href }: NavigationItem) => (
            <Link
              key={href ?? "#"}
              to={href ?? "#"}
              className="dashboard-card group flex items-center gap-4 p-4 transition-all border border-gray-200 rounded-lg shadow hover:bg-gray-100 focus:ring-2 focus:ring-blue-400 focus:outline-none"
              aria-label={`Ir a ${label}`}
              tabIndex={0}
            >
              {Icon ? (
                <Icon className="w-8 h-8 text-primary group-hover:text-primary-foreground transition-colors" />
              ) : (
                <div className="w-8 h-8" />
              )}
              <span className="text-xl font-medium">{label}</span>
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
});

export default Dashboard;
