// src/componentes/Layout.tsx
import React from "react";
import Encabezado from "../comunes/Encabezado";

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div>
      {/* Encabezado visible en todas las páginas */}
      <Encabezado />
      {/* Contenido dinámico de las rutas */}
      <main style={{ padding: "1rem", marginTop: "4rem" }}>{children}</main>
    </div>
  );
};

export default Layout;
