// src/componentes/Layout.tsx
import React from "react";
import Encabezado from "../comunes/Encabezado";

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div>
      <Encabezado />
      <section
        className="main-content"
        style={{ padding: "1rem", marginTop: "4rem" }}
      >
        {children}
      </section>
    </div>
  );
};

export default Layout;
