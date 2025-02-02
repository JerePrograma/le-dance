import type React from "react";
import { useState } from "react";
import { useAuth } from "../hooks/context/authContext";
import Boton from "../componentes/comunes/Boton";

const Login: React.FC = () => {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(email, contrasena);
      window.location.href = "/";
    } catch (err) {
      setError("Credenciales incorrectas. Intenta nuevamente.");
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <div className="w-full max-w-md p-8 space-y-8 bg-card rounded-xl shadow-lg">
        <h1 className="text-2xl font-bold text-center text-foreground">
          Iniciar Sesión
        </h1>

        <form className="space-y-6" onSubmit={handleLogin}>
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-foreground"
            >
              Email:
            </label>
            <input
              id="email"
              className="mt-1 block w-full px-3 py-2 bg-background border border-input rounded-md text-sm shadow-sm placeholder-muted-foreground
                focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-foreground"
            >
              Contraseña:
            </label>
            <input
              id="password"
              className="mt-1 block w-full px-3 py-2 bg-background border border-input rounded-md text-sm shadow-sm placeholder-muted-foreground
                focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              type="password"
              value={contrasena}
              onChange={(e) => setContrasena(e.target.value)}
              required
            />
          </div>

          {error && <p className="text-destructive text-sm">{error}</p>}

          <Boton type="submit" className="w-full">
            Ingresar
          </Boton>
        </form>

        <a
          href="/registro"
          className="block text-center text-sm text-primary hover:underline"
        >
          ¿No tienes cuenta? Regístrate aquí
        </a>
      </div>
    </div>
  );
};

export default Login;
