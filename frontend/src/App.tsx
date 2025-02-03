/***********************************************
 * src/App.tsx
 ***********************************************/
import { BrowserRouter } from "react-router-dom"; // Importar BrowserRouter aquí
import AppRouter from "./rutas/AppRouter";
import { AuthProvider } from "./hooks/context/authContext";
import "./diseño/global.css";

function App() {
  return (
    // El BrowserRouter envuelve a todo tu árbol de componentes
    <BrowserRouter>
      {/* El AuthProvider se ubica dentro del BrowserRouter */}
      <AuthProvider>
        <div className="App">
          <AppRouter />
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
