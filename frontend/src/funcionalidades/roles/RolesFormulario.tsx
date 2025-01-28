import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";

interface Rol {
  id?: number;
  descripcion: string;
}

const RolesFormulario: React.FC = () => {
  const [rol, setRol] = useState<Rol>({
    descripcion: "",
  });

  const [mensaje, setMensaje] = useState<string>(""); // Mensajes informativos
  const [searchParams] = useSearchParams(); // Para capturar "id" en URL
  const navigate = useNavigate();

  // Cargar datos si hay "id" en la URL
  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id);
    }
  }, [searchParams]);

  // Manejar cambios en los campos del formulario
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setRol({ ...rol, [name]: value });
  };

  // Buscar rol por ID
  const handleBuscar = async (id?: string) => {
    try {
      const response = await api.get<Rol>(`/api/roles/${id}`);
      setRol(response.data);
      setMensaje("Rol cargado correctamente.");
    } catch {
      setMensaje("Error al cargar los datos del rol.");
    }
  };

  // Guardar o actualizar rol
  const handleGuardar = async () => {
    if (!rol.descripcion) {
      setMensaje("Por favor, complete el campo de descripción.");
      return;
    }

    try {
      if (rol.id) {
        await api.put(`/api/roles/${rol.id}`, rol);
        setMensaje("Rol actualizado correctamente.");
      } else {
        await api.post("/api/roles", rol);
        setMensaje("Rol creado correctamente.");
      }
    } catch {
      setMensaje("Error al guardar los datos del rol.");
    }
  };

  // Limpiar formulario
  const handleLimpiar = () => {
    setRol({ descripcion: "" });
    setMensaje("");
  };

  // Volver al listado de roles
  const handleVolverListado = () => {
    navigate("/roles");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Roles</h1>

      <div className="form-grid">
        <input
          type="text"
          name="descripcion"
          value={rol.descripcion}
          onChange={handleChange}
          placeholder="Descripción del Rol (Ej. Administrador)"
          className="form-input"
        />
      </div>

      <div className="form-acciones">
        <Boton onClick={handleGuardar}>Guardar</Boton>
        <Boton onClick={handleLimpiar}>Limpiar</Boton>
        <Boton onClick={handleVolverListado}>Volver al Listado</Boton>
      </div>

      {mensaje && <p className="form-mensaje">{mensaje}</p>}
    </div>
  );
};

export default RolesFormulario;
