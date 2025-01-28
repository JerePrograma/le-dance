import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";

interface Asistencia {
  id?: number;
  fecha: string;
  presente: boolean;
  observacion: string;
}

interface Alumno {
  id: number;
  nombre: string;
  apellido: string;
}

interface Disciplina {
  id: number;
  nombre: string;
}

const AsistenciasFormulario: React.FC = () => {
  const [asistencia, setAsistencia] = useState<Asistencia>({
    fecha: "",
    presente: true,
    observacion: "",
  });

  const [alumnos, setAlumnos] = useState<Alumno[]>([]);
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const [alumnosFiltrados, setAlumnosFiltrados] = useState<Alumno[]>([]);
  const [disciplinaSeleccionada, setDisciplinaSeleccionada] = useState<
    number | null
  >(null);

  const [mensaje, setMensaje] = useState<string>("");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // Cargar datos de la asistencia si "id" está en la URL
  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id);
    }
  }, [searchParams]);

  // Cargar lista inicial de alumnos y disciplinas
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [alumnosResponse, disciplinasResponse] = await Promise.all([
          api.get<Alumno[]>("/api/alumnos/listado"),
          api.get<Disciplina[]>("/api/disciplinas"),
        ]);
        setAlumnos(alumnosResponse.data);
        setAlumnosFiltrados(alumnosResponse.data); // Mostrar todos los alumnos al inicio
        setDisciplinas(disciplinasResponse.data); // Mostrar todas las disciplinas
      } catch {
        setMensaje("Error al cargar alumnos o disciplinas.");
      }
    };
    fetchData();
  }, []);

  // Manejar cambio de fecha
  const handleFechaChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const fecha = e.target.value;
    setAsistencia({ ...asistencia, fecha });
    setDisciplinaSeleccionada(null);

    try {
      const response = await api.get<Disciplina[]>(
        `/api/disciplinas/por-fecha?fecha=${fecha}`
      );
      setDisciplinas(response.data);
      setMensaje("");
    } catch {
      setMensaje("Error al cargar disciplinas para la fecha seleccionada.");
      setDisciplinas([]);
    }
  };

  // Manejar cambio de disciplina
  const handleDisciplinaChange = async (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const disciplinaId = Number(e.target.value);
    setDisciplinaSeleccionada(disciplinaId);

    try {
      const response = await api.get<Alumno[]>(
        `/api/alumnos/por-fecha-y-disciplina?fecha=${asistencia.fecha}&disciplinaId=${disciplinaId}`
      );
      setAlumnosFiltrados(response.data);
      setMensaje("");
    } catch {
      setMensaje("Error al cargar alumnos para la disciplina seleccionada.");
      setAlumnosFiltrados([]);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const target = e.target as HTMLInputElement;
    const { name, value, type, checked } = target;
    setAsistencia({
      ...asistencia,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  const handleBuscar = async (id?: string) => {
    try {
      const response = await api.get<Asistencia>(`/api/asistencias/${id}`);
      setAsistencia(response.data);
      setMensaje("Asistencia cargada correctamente.");
    } catch {
      setMensaje("Error al cargar la asistencia.");
    }
  };

  const handleGuardar = async () => {
    try {
      await api.post("/api/asistencias", asistencia);
      setMensaje("Asistencia creada correctamente.");
    } catch {
      setMensaje("Error al guardar la asistencia.");
    }
  };

  const handleLimpiar = () => {
    setAsistencia({
      fecha: "",
      presente: true,
      observacion: "",
    });
    setDisciplinaSeleccionada(null);
    setAlumnosFiltrados(alumnos);
    setMensaje("");
  };

  const handleVolverListado = () => {
    navigate("/asistencias");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Asistencias</h1>
      <div className="form-grid">
        <input
          type="date"
          name="fecha"
          value={asistencia.fecha}
          onChange={handleFechaChange}
          className="form-input"
        />
        <select
          value={disciplinaSeleccionada || ""}
          onChange={handleDisciplinaChange}
          className="form-input"
        >
          <option value="" disabled>
            Seleccione una Disciplina
          </option>
          {disciplinas.map((disciplina) => (
            <option key={disciplina.id} value={disciplina.id}>
              {`${disciplina.id} - ${disciplina.nombre}`}
            </option>
          ))}
        </select>
        <select className="form-input">
          <option value="" disabled>
            Seleccione un Alumno
          </option>
          {alumnosFiltrados.map((alumno) => (
            <option key={alumno.id} value={alumno.id}>
              {`${alumno.nombre} ${alumno.apellido}`}
            </option>
          ))}
        </select>
        <textarea
          name="observacion"
          value={asistencia.observacion}
          onChange={handleChange}
          placeholder="Observaciones"
          className="form-input"
        ></textarea>
        <label className="form-checkbox">
          <input
            type="checkbox"
            name="presente"
            checked={asistencia.presente}
            onChange={handleChange}
          />
          Presente
        </label>
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

export default AsistenciasFormulario;
