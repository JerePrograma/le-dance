// src/funcionalidades/alumnos/AlumnosFormulario.tsx
import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { alumnoEsquema } from "../../validaciones/alumnoEsquema";
import alumnosApi from "../../utilidades/alumnosApi";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import { toast } from "react-toastify";
import {
  AlumnoListadoResponse,
  AlumnoRequest,
  AlumnoResponse,
  InscripcionResponse,
} from "../../types/types";
import useDebounce from "../../hooks/useDebounce";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";

// Valores iniciales para el formulario de alumno
const initialAlumnoValues: AlumnoRequest = {
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
};

const AlumnosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [alumnoId, setAlumnoId] = useState<number | null>(null);
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoListadoResponse[]
  >([]);

  // Debounce para búsqueda por nombre (300ms)
  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  const handleBuscar = useCallback(
    async (idStr: string, resetForm: (values: AlumnoRequest) => void) => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          setMensaje("ID inválido");
          resetForm(initialAlumnoValues);
          return;
        }
        const data: AlumnoResponse = await alumnosApi.obtenerAlumnoPorId(idNum);
        resetForm({
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
        await cargarInscripcionesDelAlumno(data.id);
        setMensaje("Alumno cargado correctamente.");
      } catch (error) {
        console.error(error);
        setMensaje("No se encontró un alumno con ese ID.");
        resetForm(initialAlumnoValues);
        setAlumnoId(null);
        setInscripciones([]);
      }
    },
    []
  );

  const cargarInscripcionesDelAlumno = async (id: number) => {
    try {
      const data = await inscripcionesApi.listarInscripcionesPorAlumno(id);
      setInscripciones(data);
    } catch (err) {
      console.error("Error al cargar inscripciones:", err);
      setInscripciones([]);
    }
  };

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) {
      handleBuscar(idParam, () => {});
    }
  }, [searchParams, handleBuscar]);

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
    if (debouncedNombreBusqueda) {
      buscarAlumnosPorNombre(debouncedNombreBusqueda);
    } else {
      setSugerenciasAlumnos([]);
    }
  }, [debouncedNombreBusqueda, buscarAlumnosPorNombre]);

  const handleSeleccionarAlumno = useCallback(
    async (
      id: number,
      nombreCompleto: string,
      resetForm: (values: AlumnoRequest) => void
    ) => {
      setNombreBusqueda(nombreCompleto);
      setIdBusqueda(id.toString());
      await handleBuscar(id.toString(), resetForm);
      setSugerenciasAlumnos([]);
    },
    [handleBuscar]
  );

  const handleGuardarAlumno = useCallback(
    async (values: AlumnoRequest) => {
      try {
        if (alumnoId) {
          await alumnosApi.actualizarAlumno(alumnoId, values);
          toast.success("Alumno actualizado correctamente.");
        } else {
          const resp = await alumnosApi.registrarAlumno(values);
          setAlumnoId(resp.id);
          toast.success("Alumno creado correctamente.");
        }
      } catch (error) {
        toast.error("Error al guardar datos del alumno.");
      }
    },
    [alumnoId]
  );

  const handleEliminarInscripcion = useCallback(
    async (inscripcionId: number) => {
      const confirmacion = window.confirm(
        "¿Estás seguro de eliminar esta inscripción?"
      );
      if (!confirmacion) return;
      try {
        await inscripcionesApi.eliminarInscripcion(inscripcionId);
        setInscripciones((prev) =>
          prev.filter((ins) => ins.id !== inscripcionId)
        );
        toast.success("Inscripción eliminada correctamente.");
      } catch (error) {
        console.error("Error al eliminar la inscripción:", error);
        toast.error("No se pudo eliminar la inscripción.");
      }
    },
    []
  );

  // Hasta aquí termina la parte de inicialización y lógica (antes del return)

  return (
    <div className="formulario">
      <h1 className="form-title">Ficha de Alumno</h1>
      <Formik
        initialValues={initialAlumnoValues}
        validationSchema={alumnoEsquema}
        onSubmit={handleGuardarAlumno}
        enableReinitialize
      >
        {({ resetForm, isSubmitting }) => (
          <Form className="formulario">
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
              <Boton
                onClick={() =>
                  handleBuscar(idBusqueda, (vals) =>
                    resetForm({ values: vals })
                  )
                }
              >
                Buscar
              </Boton>
            </div>

            {/* Búsqueda por Nombre con sugerencias */}
            <div className="form-busqueda">
              <label htmlFor="nombreBusqueda">Buscar por Nombre:</label>
              <input
                type="text"
                id="nombreBusqueda"
                value={nombreBusqueda}
                onChange={(e) => setNombreBusqueda(e.target.value)}
                className="form-input"
              />
              {sugerenciasAlumnos.length > 0 && (
                <ul className="sugerencias-lista">
                  {sugerenciasAlumnos.map((alumno) => (
                    <li
                      key={alumno.id}
                      onClick={() =>
                        handleSeleccionarAlumno(
                          alumno.id,
                          `${alumno.nombre} ${alumno.apellido}`,
                          (vals) => resetForm({ values: vals })
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
              <button
                onClick={() => setNombreBusqueda("")}
                className="limpiar-boton"
              >
                Limpiar
              </button>
            )}

            <fieldset className="form-fieldset">
              <legend>Datos Personales</legend>
              <div className="form-grid">
                {[
                  { name: "nombre", label: "Nombre (obligatorio)" },
                  { name: "apellido", label: "Apellido" },
                  {
                    name: "fechaNacimiento",
                    label: "Fecha de Nacimiento",
                    type: "date",
                  },
                  {
                    name: "fechaIncorporacion",
                    label: "Fecha de Incorporación",
                    type: "date",
                  },
                  { name: "celular1", label: "Celular 1 (obligatorio)" },
                  { name: "celular2", label: "Celular 2" },
                  { name: "email1", label: "Email 1", type: "email" },
                  { name: "email2", label: "Email 2", type: "email" },
                  { name: "documento", label: "Documento" },
                  { name: "cuit", label: "CUIT" },
                  { name: "nombrePadres", label: "Nombre de Padres" },
                ].map(({ name, label, type = "text" }) => (
                  <div key={name}>
                    <label>{label}:</label>
                    <Field name={name} type={type} className="form-input" />
                    <ErrorMessage
                      name={name}
                      component="div"
                      className="error"
                    />
                  </div>
                ))}
                {[
                  {
                    name: "autorizadoParaSalirSolo",
                    label: "Autorizado para salir solo",
                  },
                  { name: "activo", label: "Activo" },
                ].map(({ name, label }) => (
                  <div key={name}>
                    <label>{label}:</label>
                    <Field
                      name={name}
                      type="checkbox"
                      className="form-checkbox"
                    />
                  </div>
                ))}
              </div>
            </fieldset>

            <div>
              <label>Otras Notas:</label>
              <Field as="textarea" name="otrasNotas" className="form-input" />
              <ErrorMessage
                name="otrasNotas"
                component="div"
                className="error"
              />
            </div>

            <div className="form-acciones">
              <button
                type="submit"
                disabled={isSubmitting}
                className="form-boton"
              >
                Guardar Alumno
              </button>
              <button
                type="reset"
                className="form-botonSecundario"
                onClick={() => resetForm({ values: initialAlumnoValues })}
              >
                Limpiar
              </button>
              <button
                type="button"
                className="form-botonSecundario"
                onClick={() => navigate("/alumnos")}
              >
                Volver al Listado
              </button>
            </div>

            <fieldset className="form-fieldset">
              <legend className="form-legend text-xl font-semibold">
                Inscripciones del Alumno
              </legend>
              {alumnoId ? (
                <>
                  <div className="flex justify-end mb-4">
                    <Boton
                      onClick={() =>
                        navigate(
                          `/inscripciones/formulario?alumnoId=${alumnoId}`
                        )
                      }
                    >
                      Agregar Disciplina
                    </Boton>
                  </div>
                  <Tabla
                    encabezados={[
                      "ID",
                      "Disciplina",
                      "Bonificación",
                      "Costo",
                      "Notas",
                      "Acciones",
                    ]}
                    datos={inscripciones}
                    extraRender={(fila) => [
                      fila.id,
                      fila.disciplina.nombre,
                      fila.bonificacion ? fila.bonificacion.descripcion : "N/A",
                      fila.costoParticular
                        ? `$${fila.costoParticular.toFixed(2)}`
                        : "-",
                      fila.notas || "-",
                    ]}
                    acciones={(fila) => (
                      <div className="flex flex-col sm:flex-row gap-2">
                        <Boton
                          onClick={() =>
                            navigate(`/inscripciones/formulario?id=${fila.id}`)
                          }
                          secondary
                        >
                          Editar
                        </Boton>
                        <Boton
                          onClick={() => handleEliminarInscripcion(fila.id)}
                          className="bg-red-500 text-white hover:bg-red-600"
                        >
                          Eliminar
                        </Boton>
                      </div>
                    )}
                  />
                </>
              ) : (
                <p className="text-gray-500 text-sm mt-4">
                  No se pueden gestionar inscripciones hasta que{" "}
                  <strong>se guarde</strong> un alumno.
                </p>
              )}
              {mensaje && (
                <p className="form-mensaje form-mensaje-error">{mensaje}</p>
              )}
            </fieldset>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AlumnosFormulario;
