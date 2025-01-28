import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";

interface Disciplina {
  id?: number;
  nombre: string;
  horario: string;
  frecuenciaSemanal?: number;
  duracion: string;
  salon: string;
  valorCuota?: number;
  matricula?: number;
  profesorId?: number;
  cupoMaximo?: number;
}

interface Profesor {
  id: number;
  nombreCompleto: string;
}

const DisciplinasFormulario: React.FC = () => {
  const [disciplina, setDisciplina] = useState<Disciplina>({
    nombre: "",
    horario: "",
    frecuenciaSemanal: undefined,
    duracion: "",
    salon: "",
    valorCuota: undefined,
    matricula: undefined,
    profesorId: undefined,
    cupoMaximo: undefined,
  });

  const [profesores, setProfesores] = useState<Profesor[]>([]);
  const [mensaje, setMensaje] = useState<string>("");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id);
    }
  }, [searchParams]);

  useEffect(() => {
    const fetchProfesores = async () => {
      try {
        const response = await api.get<Profesor[]>(
          "/api/profesores/simplificados"
        );
        setProfesores(response.data);
      } catch {
        setMensaje("Error al cargar la lista de profesores.");
      }
    };

    fetchProfesores();
  }, []);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;

    setDisciplina({
      ...disciplina,
      [name]: value === "" || isNaN(Number(value)) ? value : Number(value),
    });
  };

  const handleBuscar = async (id?: string) => {
    try {
      const response = await api.get<Disciplina>(`/api/disciplinas/${id}`);
      setDisciplina(response.data);
      setMensaje("Disciplina cargada correctamente.");
    } catch {
      setMensaje("Error al cargar la disciplina.");
    }
  };

  const handleGuardar = async () => {
    try {
      if (disciplina.id) {
        await api.put(`/api/disciplinas/${disciplina.id}`, disciplina);
        setMensaje("Disciplina actualizada correctamente.");
      } else {
        await api.post("/api/disciplinas", disciplina);
        setMensaje("Disciplina creada correctamente.");
      }
    } catch {
      setMensaje("Error al guardar la disciplina.");
    }
  };

  const handleLimpiar = () => {
    setDisciplina({
      nombre: "",
      horario: "",
      frecuenciaSemanal: undefined,
      duracion: "",
      salon: "",
      valorCuota: undefined,
      matricula: undefined,
      profesorId: undefined,
      cupoMaximo: undefined,
    });
    setMensaje("");
  };

  const handleVolverListado = () => {
    navigate("/disciplinas");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Disciplinas</h1>
      <div className="form-grid">
        <input
          type="text"
          name="nombre"
          value={disciplina.nombre}
          onChange={handleChange}
          placeholder="Ej. Yoga, Baile Moderno"
          className="form-input"
        />
        <input
          type="text"
          name="horario"
          value={disciplina.horario}
          onChange={handleChange}
          placeholder="Ej. Lunes y Miércoles 18:00 - 19:00"
          className="form-input"
        />
        <input
          type="number"
          name="frecuenciaSemanal"
          value={disciplina.frecuenciaSemanal || ""}
          onChange={handleChange}
          placeholder="Frecuencia Semanal (Ej. 2)"
          className="form-input"
        />
        <input
          type="text"
          name="duracion"
          value={disciplina.duracion}
          onChange={handleChange}
          placeholder="Duración (Ej. 1 hora)"
          className="form-input"
        />
        <input
          type="text"
          name="salon"
          value={disciplina.salon}
          onChange={handleChange}
          placeholder="Salón (Ej. Salón A)"
          className="form-input"
        />
        <input
          type="number"
          name="valorCuota"
          value={disciplina.valorCuota || ""}
          onChange={handleChange}
          placeholder="Valor Cuota (Ej. 1500)"
          className="form-input"
        />
        <input
          type="number"
          name="matricula"
          value={disciplina.matricula || ""}
          onChange={handleChange}
          placeholder="Matrícula (Ej. 500)"
          className="form-input"
        />
        <select
          name="profesorId"
          value={disciplina.profesorId || ""}
          onChange={handleChange}
          className="form-input"
        >
          <option value="" disabled>
            Seleccione un Profesor
          </option>
          {profesores.map((profesor) => (
            <option key={profesor.id} value={profesor.id}>
              {profesor.nombreCompleto}
            </option>
          ))}
        </select>
        <input
          type="number"
          name="cupoMaximo"
          value={disciplina.cupoMaximo || ""}
          onChange={handleChange}
          placeholder="Cupo Máximo de Alumnos"
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

export default DisciplinasFormulario;
