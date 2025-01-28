import React, { useState, useEffect } from "react";
import api from "../../utilidades/axiosConfig";

interface Rol {
  id: number;
  descripcion: string;
}

const UsuariosFormulario: React.FC = () => {
  const [email, setEmail] = useState("");
  const [nombreUsuario, setNombreUsuario] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [rol, setRol] = useState("");
  const [roles, setRoles] = useState<Rol[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const response = await api.get<Rol[]>("/api/roles");
        setRoles(response.data);
      } catch (err) {
        console.error("Error al cargar los roles:", err);
      }
    };
    fetchRoles();
  }, []);

  const handleRegistro = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.post("/api/usuarios/registro", {
        email,
        nombreUsuario,
        contrasena,
        rol,
      });
      window.location.href = "/login"; // Redirige al login tras registro exitoso
    } catch (err) {
      setError("Error al registrar el usuario. Verifica los datos.");
    }
  };

  return (
    <div className="auth-container">
      <h1 className="auth-title">Registro</h1>

      <form className="auth-form" onSubmit={handleRegistro}>
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
          <label className="auth-label">Nombre de Usuario:</label>
          <input
            className="auth-input"
            type="text"
            value={nombreUsuario}
            onChange={(e) => setNombreUsuario(e.target.value)}
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

        <div>
          <label className="auth-label">Rol:</label>
          <select
            className="auth-input"
            value={rol}
            onChange={(e) => setRol(e.target.value)}
            required
          >
            <option value="" disabled>
              Seleccione un rol
            </option>
            {roles.map((rol) => (
              <option key={rol.id} value={rol.descripcion}>
                {rol.descripcion}
              </option>
            ))}
          </select>
        </div>

        {error && <p className="auth-error">{error}</p>}

        <button className="auth-button" type="submit">
          Registrarse
        </button>
      </form>

      <a href="/login" className="auth-link">
        ¿Ya tienes cuenta? Inicia sesión aquí
      </a>
    </div>
  );
};

export default UsuariosFormulario;
