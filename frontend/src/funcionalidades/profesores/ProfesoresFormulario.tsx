import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";

interface Profesor {
  id?: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const ProfesoresFormulario: React.FC = () => {
  const [profesor, setProfesor] = useState<Profesor>({
    nombre: "",
    apellido: "",
    especialidad: "",
    aniosExperiencia: 0,
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
  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setProfesor({ ...profesor, [name]: value });
  };

  // Buscar profesor por ID
  const handleBuscar = async (id?: string) => {
    try {
      const response = await api.get<Profesor>(`/api/profesores/${id}`);
      setProfesor(response.data);
      setMensaje("Profesor cargado correctamente.");
    } catch {
      setMensaje("Error al cargar los datos del profesor.");
    }
  };

  // Guardar o actualizar profesor
  const handleGuardar = async () => {
    if (!profesor.nombre || !profesor.apellido || !profesor.especialidad) {
      setMensaje("Por favor, complete todos los campos obligatorios.");
      return;
    }

    try {
      if (profesor.id) {
        await api.put(`/api/profesores/${profesor.id}`, profesor);
        setMensaje("Profesor actualizado correctamente.");
      } else {
        await api.post("/api/profesores", profesor);
        setMensaje("Profesor creado correctamente.");
      }
    } catch {
      setMensaje("Error al guardar los datos del profesor.");
    }
  };

  // Limpiar formulario
  const handleLimpiar = () => {
    setProfesor({
      nombre: "",
      apellido: "",
      especialidad: "",
      aniosExperiencia: 0,
    });
    setMensaje("");
  };

  // Volver al listado de profesores
  const handleVolverListado = () => {
    navigate("/profesores");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Profesores</h1>

      <div className="form-grid">
        <input
          type="text"
          name="nombre"
          value={profesor.nombre}
          onChange={handleChange}
          placeholder="Nombre (Ej. Juan)"
          className="form-input"
        />
        <input
          type="text"
          name="apellido"
          value={profesor.apellido}
          onChange={handleChange}
          placeholder="Apellido (Ej. Pérez)"
          className="form-input"
        />
        <input
          type="text"
          name="especialidad"
          value={profesor.especialidad}
          onChange={handleChange}
          placeholder="Especialidad (Ej. Baile Contemporáneo)"
          className="form-input"
        />
        <input
          type="number"
          name="aniosExperiencia"
          value={profesor.aniosExperiencia || ""}
          onChange={handleChange}
          placeholder="Años de Experiencia (Ej. 5)"
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

export default ProfesoresFormulario;
