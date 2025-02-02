import type React from "react";
import Encabezado from "./comunes/Encabezado";

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <Encabezado />
      <main className="container mx-auto px-4 py-8 mt-16">{children}</main>
    </div>
  );
};

export default Layout;
