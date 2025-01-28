import React, { useState } from "react";
import { useAuth } from "../hooks/context/authContext";

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
    <div className="auth-container">
      <h1 className="auth-title">Iniciar Sesión</h1>

      <form className="auth-form" onSubmit={handleLogin}>
        <div>
          <label className="auth-label">Email:</label>
          <input
            className="auth-input"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="auth-label">Contraseña:</label>
          <input
            className="auth-input"
            type="password"
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
            required
          />
        </div>

        {error && <p className="auth-error">{error}</p>}

        <button className="auth-button" type="submit">
          Ingresar
        </button>
      </form>

      <a href="/registro" className="auth-link">
        ¿No tienes cuenta? Regístrate aquí
      </a>
    </div>
  );
};

export default Login;
