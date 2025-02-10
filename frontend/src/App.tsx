// src/App.tsx
import { BrowserRouter } from "react-router-dom";
import AppRouter from "./rutas/AppRouter";
import { AuthProvider } from "./hooks/context/authContext";
import { AsistenciaProvider } from "./hooks/context/asistenciaContext";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AsistenciaProvider>
          <AppRouter />
        </AsistenciaProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
