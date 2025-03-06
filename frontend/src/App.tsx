import { BrowserRouter } from "react-router-dom";
import AppRouter from "./rutas/AppRouter";
import { AuthProvider } from "./hooks/context/authContext";
import { ThemeProvider } from "./hooks/context/ThemeContext";
import { SidebarProvider } from "./hooks/context/SideBarContext";
import MainLayout from "./componentes/layout/MainLayout";
import "./index.css"; // Importa el archivo de entrada con Tailwind y tus estilos personalizados

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <SidebarProvider>
            <MainLayout>
              <AppRouter />
            </MainLayout>
          </SidebarProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
