import React from "react";
import { BrowserRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "./hooks/queryClient"; // Importamos el QueryClient
import AppRouter from "./rutas/AppRouter";
import { AuthProvider } from "./hooks/context/authContext";
import { ThemeProvider } from "./hooks/context/ThemeContext";
import { SidebarProvider } from "./hooks/context/SideBarContext";
import MainLayout from "./componentes/layout/MainLayout";
import "./index.css"; // Importa Tailwind y estilos personalizados
import { ToastContainer } from "react-toastify";

const App: React.FC = () => {
  return (
    <React.StrictMode>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          {/* Colocamos BrowserRouter en la raíz */}
          <BrowserRouter>
            {/* Ahora AuthProvider y los demás componentes están dentro del contexto Router */}
            <AuthProvider>
              <SidebarProvider>
                <MainLayout>
                  <AppRouter />
                  <ToastContainer />
                </MainLayout>
              </SidebarProvider>
            </AuthProvider>
          </BrowserRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </React.StrictMode>
  );
};

export default App;
