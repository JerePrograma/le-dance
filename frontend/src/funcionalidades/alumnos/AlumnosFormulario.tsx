import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import alumnosApi from "../../utilidades/alumnosApi";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import {
  AlumnoListadoResponse,
  AlumnoRequest,
  AlumnoResponse,
  InscripcionResponse,
} from "../../types/types";

const AlumnosFormulario: React.FC = () => {
  // Estado para los datos del alumno
  const [alumnoForm, setAlumnoForm] = useState<AlumnoRequest>({
    nombre: "",
    apellido: "",
    fechaNacimiento: "",
    fechaIncorporacion: "",
    celular1: "",
    celular2: "",
    email1: "",
    email2: "",
    documento: "",
    cuit: "",
    nombrePadres: "",
    autorizadoParaSalirSolo: false,
    activo: true,
    otrasNotas: "",
    cuotaTotal: 0,
  });

  // ID del alumno (si existe)
  const [alumnoId, setAlumnoId] = useState<number | null>(null);

  // Manejo de inscripciones
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);

  // Para feedback
  const [idBusqueda, setIdBusqueda] = useState("");
  const [mensaje, setMensaje] = useState("");

  // Hooks de routing
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoListadoResponse[]
  >([]);

  const buscarAlumnosPorNombre = useCallback(async (nombre: string) => {
    if (nombre.length < 2) {
      setSugerenciasAlumnos([]);
      return;
    }
    try {
      const response = await alumnosApi.buscarPorNombre(nombre);
      setSugerenciasAlumnos(response);
    } catch (error) {
      console.error("Error al buscar alumnos:", error);
      setSugerenciasAlumnos([]);
    }
  }, []);

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      if (nombreBusqueda.length >= 2) {
        buscarAlumnosPorNombre(nombreBusqueda);
      }
    }, 300);

    return () => clearTimeout(delayDebounceFn);
  }, [nombreBusqueda, buscarAlumnosPorNombre]);

  const handleSeleccionarAlumno = async (
    id: number,
    nombreCompleto: string
  ) => {
    setNombreBusqueda(nombreCompleto); // Se muestra el nombre completo en el campo
    setIdBusqueda(id.toString()); // Se actualiza el campo de búsqueda por ID
    await handleBuscar(id.toString()); // Carga la ficha completa del alumno
    setSugerenciasAlumnos([]); // Limpia las sugerencias
  };

  useEffect(() => {
    if (nombreBusqueda.length < 2) {
      setSugerenciasAlumnos([]);
    }
  }, [nombreBusqueda]);

  // ================================
  // CARGAR ALUMNO
  // ================================
  const handleBuscar = useCallback(async (idStr: string) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID inválido");
        return;
      }
      const data: AlumnoResponse = await alumnosApi.obtenerAlumnoPorId(idNum);

      setAlumnoForm({
        nombre: data.nombre,
        apellido: data.apellido,
        fechaNacimiento: data.fechaNacimiento || "",
        fechaIncorporacion: data.fechaIncorporacion || "",
        celular1: data.celular1 || "",
        celular2: data.celular2 || "",
        email1: data.email1 || "",
        email2: data.email2 || "",
        documento: data.documento || "",
        cuit: data.cuit || "",
        nombrePadres: data.nombrePadres || "",
        autorizadoParaSalirSolo: data.autorizadoParaSalirSolo || false,
        activo: data.activo ?? true,
        otrasNotas: data.otrasNotas || "",
        cuotaTotal: data.cuotaTotal || 0,
      });
      setAlumnoId(data.id);

      // Cargar también sus inscripciones
      await cargarInscripcionesDelAlumno(data.id);

      setMensaje("Alumno cargado correctamente.");
    } catch (error) {
      console.error(error);
      handleLimpiar();
      setMensaje("No se encontró un alumno con ese ID.");
    }
  }, []);

  const cargarInscripcionesDelAlumno = async (id: number) => {
    try {
      // asumiendo que inscripcionesApi tiene un método para listar por alumno
      const data = await inscripcionesApi.listarInscripcionesPorAlumno(id);
      setInscripciones(data);
    } catch (err) {
      console.error("Error al cargar inscripciones del alumno:", err);
      setInscripciones([]);
    }
  };

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) handleBuscar(idParam);
  }, [searchParams, handleBuscar]);

  // ================================
  // MANEJO DEL FORM ALUMNO
  // ================================
  const handleChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value } = e.target;
    setAlumnoForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setAlumnoForm((prev) => ({ ...prev, [name]: checked }));
  };

  const handleGuardarAlumno = async () => {
    if (!alumnoForm.nombre || !alumnoForm.celular1) {
      setMensaje(
        "Por favor, completa los campos obligatorios (nombre, celular1)."
      );
      return;
    }
    try {
      let resp: AlumnoResponse;
      if (alumnoId) {
        resp = await alumnosApi.actualizarAlumno(alumnoId, alumnoForm);
        setMensaje("Alumno actualizado correctamente.");
      } else {
        resp = await alumnosApi.registrarAlumno(alumnoForm);
        setMensaje("Alumno creado correctamente.");
      }
      setAlumnoId(resp.id);
      // recargar inscripciones en caso de que se cree un nuevo ID
      if (resp.id) await cargarInscripcionesDelAlumno(resp.id);
    } catch (error) {
      console.error(error);
      setMensaje("Error al guardar datos del alumno.");
    }
  };

  const handleLimpiar = () => {
    setAlumnoForm({
      nombre: "",
      apellido: "",
      fechaNacimiento: "",
      fechaIncorporacion: "",
      celular1: "",
      celular2: "",
      email1: "",
      email2: "",
      documento: "",
      cuit: "",
      nombrePadres: "",
      autorizadoParaSalirSolo: false,
      activo: true,
      otrasNotas: "",
      cuotaTotal: 0,
    });
    setAlumnoId(null);
    setIdBusqueda("");
    setMensaje("");
    setInscripciones([]);
  };

  const handleVolverListado = () => {
    navigate("/alumnos");
  };

  // ================================
  // MANEJO DE INSCRIPCIONES
  // ================================

  // Ejemplo: Para crear/editar una inscripción, podrías abrir un formulario modal o algo similar.
  // Aquí, daremos un ejemplo de "Crear inscripcion" sin un pop-up:
  const handleNuevaInscripcion = () => {
    if (!alumnoId) {
      setMensaje("Primero guarda o busca un alumno para asignar disciplinas.");
      return;
    }
    navigate(`/inscripciones/formulario?alumnoId=${alumnoId}`);
  };

  const handleEditarInscripcion = (insId: number) => {
    navigate(`/inscripciones/formulario?id=${insId}`);
  };

  const handleEliminarInscripcion = async (insId: number) => {
    try {
      await inscripcionesApi.eliminarInscripcion(insId);
      setInscripciones((prev) => prev.filter((ins) => ins.id !== insId));
      setMensaje("Inscripción eliminada.");
    } catch (err) {
      console.error(err);
      setMensaje("Error al eliminar inscripción.");
    }
  };

  // ================================
  // RENDER
  // ================================
  return (
    <div className="formulario">
      <h1 className="form-title">Ficha de Alumno</h1>

      {/* Búsqueda por ID */}
      <div className="form-busqueda">
        <label htmlFor="idBusqueda">Número de Alumno:</label>
        <input
          type="number"
          id="idBusqueda"
          value={idBusqueda}
          onChange={(e) => setIdBusqueda(e.target.value)}
          className="form-input"
        />
        <Boton onClick={() => handleBuscar(idBusqueda)}>Buscar</Boton>
      </div>
      {/* Nueva búsqueda por nombre con sugerencias */}
      <div className="form-busqueda">
        <label htmlFor="nombreBusqueda">Buscar por Nombre:</label>
        <input
          type="text"
          id="nombreBusqueda"
          value={nombreBusqueda}
          onChange={(e) => {
            setNombreBusqueda(e.target.value);
            buscarAlumnosPorNombre(e.target.value);
          }}
          className="form-input"
        />
        {/* Sugerencias dinámicas */}
        {sugerenciasAlumnos.length > 0 && (
          <ul className="sugerencias-lista">
            {sugerenciasAlumnos.map((alumno) => (
              <li
                key={alumno.id}
                onClick={() =>
                  handleSeleccionarAlumno(
                    alumno.id,
                    `${alumno.nombre} ${alumno.apellido}`
                  )
                }
                className="sugerencia-item"
              >
                <strong>{alumno.nombre}</strong> {alumno.apellido}
              </li>
            ))}
          </ul>
        )}
      </div>

      {nombreBusqueda && (
        <button onClick={() => setNombreBusqueda("")} className="limpiar-boton">
          Limpiar
        </button>
      )}

      <fieldset className="form-fieldset">
        <legend>Datos Personales</legend>
        <div className="form-grid">
          {/* Campos del Alumno */}
          <div>
            <label>Nombre (obligatorio):</label>
            <input
              name="nombre"
              type="text"
              value={alumnoForm.nombre}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Apellido:</label>
            <input
              name="apellido"
              type="text"
              value={alumnoForm.apellido}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Fecha de Nacimiento:</label>
            <input
              name="fechaNacimiento"
              type="date"
              value={alumnoForm.fechaNacimiento || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Fecha de Incorporación:</label>
            <input
              name="fechaIncorporacion"
              type="date"
              value={alumnoForm.fechaIncorporacion || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Celular 1 (obligatorio):</label>
            <input
              name="celular1"
              type="text"
              value={alumnoForm.celular1 || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Celular 2:</label>
            <input
              name="celular2"
              type="text"
              value={alumnoForm.celular2 || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Email 1:</label>
            <input
              name="email1"
              type="email"
              value={alumnoForm.email1 || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Email 2:</label>
            <input
              name="email2"
              type="email"
              value={alumnoForm.email2 || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Documento:</label>
            <input
              name="documento"
              type="text"
              value={alumnoForm.documento || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>CUIT:</label>
            <input
              name="cuit"
              type="text"
              value={alumnoForm.cuit || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Nombre de Padres:</label>
            <input
              name="nombrePadres"
              type="text"
              value={alumnoForm.nombrePadres || ""}
              onChange={handleChange}
              className="form-input"
            />
          </div>
          <div>
            <label>Autorizado para salir solo:</label>
            <input
              name="autorizadoParaSalirSolo"
              type="checkbox"
              checked={alumnoForm.autorizadoParaSalirSolo || false}
              onChange={handleCheckboxChange}
            />
          </div>
          <div>
            <label>Activo:</label>
            <input
              name="activo"
              type="checkbox"
              checked={alumnoForm.activo || false}
              onChange={handleCheckboxChange}
            />
          </div>
        </div>
      </fieldset>

      {/* Otras notas */}
      <div>
        <label>Otras Notas:</label>
        <textarea
          name="otrasNotas"
          value={alumnoForm.otrasNotas || ""}
          onChange={handleChange}
          className="form-input"
        />
      </div>

      <div className="form-acciones">
        <button className="form-boton" onClick={handleGuardarAlumno}>
          Guardar Alumno
        </button>
        <button className="form-botonSecundario" onClick={handleLimpiar}>
          Limpiar
        </button>
        <button className="form-botonSecundario" onClick={handleVolverListado}>
          Volver al Listado
        </button>
      </div>

      {/* ================================
          SECCION DE INSCRIPCIONES
         ================================ */}
      <h2>Inscripciones del Alumno</h2>
      {alumnoId ? (
        <>
          <button
            className="botones botones-primario mb-4"
            onClick={handleNuevaInscripcion}
          >
            Agregar Disciplina
          </button>

          <Tabla
            encabezados={[
              "ID",
              "DisciplinaID",
              "BonificaciónID",
              "Costo Particular",
              "Notas",
              "Acciones",
            ]}
            datos={inscripciones}
            acciones={(fila) => (
              <div className="flex gap-2">
                <button
                  onClick={() => handleEditarInscripcion(fila.id)}
                  className="botones botones-primario"
                >
                  Editar
                </button>
                <button
                  onClick={() => handleEliminarInscripcion(fila.id)}
                  className="botones bg-red-500 text-white hover:bg-red-600"
                >
                  Eliminar
                </button>
              </div>
            )}
            extraRender={(fila) => [
              fila.id,
              fila.disciplinaId,
              fila.bonificacionId ?? "N/A",
              fila.costoParticular ?? 0,
              fila.notas ?? "",
            ]}
          />
        </>
      ) : (
        <p>
          No se puede gestionar inscripciones hasta que{" "}
          <strong>guarde o cargue</strong> un Alumno.
        </p>
      )}

      {mensaje && <p className="form-mensaje form-mensaje-error">{mensaje}</p>}
    </div>
  );
};

export default AlumnosFormulario;
