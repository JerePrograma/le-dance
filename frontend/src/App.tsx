import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { AuthProvider } from "./hooks/context/authContext";
import routes from "./rutas/AppRouter"; // ✅ Importa las rutas corregidas
import "./diseño/global.css";

// Crea el enrutador con las rutas definidas en AppRouter.tsx
const router = createBrowserRouter(routes);

function App() {
  return (
    <AuthProvider>
      <div className="App">
        <RouterProvider router={router} />
      </div>
    </AuthProvider>
  );
}

export default App;
