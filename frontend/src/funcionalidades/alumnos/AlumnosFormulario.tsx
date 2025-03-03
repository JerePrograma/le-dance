// src/funcionalidades/alumnos/AlumnosFormulario.tsx
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage, type FormikHelpers } from "formik";
import { alumnoEsquema } from "../../validaciones/alumnoEsquema";
import alumnosApi from "../../api/alumnosApi";
import inscripcionesApi from "../../api/inscripcionesApi";
import { toast } from "react-toastify";
import type {
  AlumnoListadoResponse,
  AlumnoRegistroRequest,
  AlumnoModificacionRequest,
  InscripcionResponse,
} from "../../types/types";
import useDebounce from "../../hooks/useDebounce";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { Search, X } from "lucide-react";
import { Button } from "../../componentes/ui/button";
import {
  convertToAlumnoRegistroRequest,
  convertToAlumnoModificacionRequest,
} from "../../utilidades/alumnoUtils";

// Pre-cargamos la fecha de incorporación con la fecha actual (formato "yyyy-MM-dd")
const today = new Date().toISOString().split("T")[0];

const initialAlumnoValues: AlumnoRegistroRequest &
  Partial<AlumnoModificacionRequest> = {
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  fechaIncorporacion: today,
  celular1: "",
  celular2: "",
  email1: "",
  documento: "",
  cuit: "",
  nombrePadres: "",
  autorizadoParaSalirSolo: false,
  otrasNotas: "",
  cuotaTotal: 0,
  disciplinas: [],
  activo: true,
};

const AlumnosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [alumnoId, setAlumnoId] = useState<number | null>(null);
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [formValues, setFormValues] = useState<AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>>(initialAlumnoValues);

  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  // Primero se declara resetearFormulario para poder usarla posteriormente
  const resetearFormulario = () => {
    setFormValues(initialAlumnoValues);
    setAlumnoId(null);
    setInscripciones([]);
  };

  // Se declara cargarInscripciones antes de usarla en otros callbacks
  const cargarInscripciones = useCallback(async (alumnoId: number | null) => {
    if (alumnoId) {
      const inscripcionesDelAlumno = await inscripcionesApi.listar(alumnoId);
      setInscripciones(inscripcionesDelAlumno);
    } else {
      setInscripciones([]);
    }
  }, []);

  const handleBuscar = useCallback(async (id: string) => {
    try {
      if (id) {
        const alumno = await alumnosApi.obtenerPorId(Number(id));
        console.log("Alumno data received:", alumno);
        // Se utiliza la función de conversión e incorpora el valor de "activo"
        const convertedAlumno = { ...convertToAlumnoRegistroRequest(alumno), activo: alumno.activo ?? true };
        console.log("Converted alumno data:", convertedAlumno);
        setFormValues(convertedAlumno);
        setAlumnoId(alumno.id);
        cargarInscripciones(alumno.id);
        setMensaje("");
      } else {
        setMensaje("Por favor, ingrese un ID de alumno.");
        resetearFormulario();
      }
    } catch (error) {
      setMensaje("Alumno no encontrado.");
      resetearFormulario();
    }
  }, [cargarInscripciones]);

  const handleSeleccionarAlumno = async (id: number, nombreCompleto: string) => {
    try {
      const alumno = await alumnosApi.obtenerPorId(id);
      const convertedAlumno = { ...convertToAlumnoRegistroRequest(alumno), activo: alumno.activo ?? true };
      setFormValues(convertedAlumno);
      setAlumnoId(alumno.id);
      setNombreBusqueda(nombreCompleto);
      cargarInscripciones(alumno.id);
      setMensaje("");
    } catch (error) {
      setMensaje("Alumno no encontrado.");
      resetearFormulario();
    }
  };

  const handleGuardarAlumno = async (
    values: AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>,
    { setSubmitting }: FormikHelpers<AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>>
  ) => {
    try {
      let successMsg: string;
      if (alumnoId) {
        await alumnosApi.actualizar(
          alumnoId,
          convertToAlumnoModificacionRequest(values as AlumnoModificacionRequest)
        );
        successMsg = "Alumno actualizado correctamente";
      } else {
        const nuevoAlumno = await alumnosApi.registrar(values as AlumnoRegistroRequest);
        setAlumnoId(nuevoAlumno.id);
        successMsg = "Alumno creado correctamente";
      }
      setMensaje(successMsg);
      toast.success(successMsg);
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message ||
        (error.response?.status === 404 ? "Alumno no encontrado" : "Error al guardar el alumno");
      setMensaje(errorMessage);
      toast.error(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    const alumnoIdParam = searchParams.get("alumnoId") || searchParams.get("id");
    if (alumnoIdParam) {
      handleBuscar(alumnoIdParam);
    }
  }, [searchParams, handleBuscar]);

  useEffect(() => {
    const buscarSugerencias = async () => {
      if (debouncedNombreBusqueda) {
        const sugerencias = await alumnosApi.buscarPorNombre(debouncedNombreBusqueda);
        setSugerenciasAlumnos(sugerencias);
      } else {
        setSugerenciasAlumnos([]);
      }
    };
    buscarSugerencias();
  }, [debouncedNombreBusqueda]);

  const handleEliminarInscripcion = async (id: number) => {
    try {
      await inscripcionesApi.eliminar(id);
      cargarInscripciones(alumnoId);
      toast.success("Inscripción eliminada correctamente");
    } catch (error) {
      toast.error("Error al eliminar la inscripción");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Ficha de Alumno</h1>
      <Formik
        initialValues={formValues}
        validationSchema={alumnoEsquema}
        onSubmit={handleGuardarAlumno}
        enableReinitialize
      >
        {({ isSubmitting, setFieldValue }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Búsqueda por ID */}
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Alumno:
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    id="idBusqueda"
                    value={idBusqueda}
                    onChange={(e) => setIdBusqueda(e.target.value)}
                    className="form-input flex-grow"
                  />
                  <Boton onClick={() => handleBuscar(idBusqueda)} className="page-button">
                    <Search className="w-5 h-5 mr-2" />
                    Buscar
                  </Boton>
                </div>
              </div>

              {/* Búsqueda por Nombre con sugerencias */}
              <div className="col-span-full mb-4">
                <label htmlFor="nombreBusqueda" className="auth-label">
                  Buscar por Nombre:
                </label>
                <div className="relative">
                  <input
                    type="text"
                    id="nombreBusqueda"
                    value={nombreBusqueda}
                    onChange={(e) => setNombreBusqueda(e.target.value)}
                    className="form-input w-full"
                  />
                  {nombreBusqueda && (
                    <Button
                      type="button"
                      onClick={() => setNombreBusqueda("")}
                      className="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
                    >
                      <X className="w-5 h-5" />
                    </Button>
                  )}
                  {sugerenciasAlumnos.length > 0 && (
                    <ul className="sugerencias-lista">
                      {sugerenciasAlumnos.map((alumno) => (
                        <li
                          key={alumno.id}
                          onClick={() =>
                            handleSeleccionarAlumno(alumno.id, `${alumno.nombre} ${alumno.apellido}`)
                          }
                          className="sugerencia-item"
                        >
                          <strong>{alumno.nombre}</strong> {alumno.apellido}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>

              {/* Datos Personales */}
              {[
                { name: "nombre", label: "Nombre" },
                { name: "apellido", label: "Apellido" },
                { name: "fechaNacimiento", label: "Fecha de Nacimiento", type: "date" },
                { name: "fechaIncorporacion", label: "Fecha de Incorporación", type: "date" },
                { name: "celular1", label: "Celular 1" },
                { name: "celular2", label: "Celular 2" },
                { name: "email1", label: "Email", type: "email" },
                { name: "documento", label: "Documento" },
                { name: "nombrePadres", label: "Nombre de Padres" },
              ].map(({ name, label, type = "text" }) => (
                <div key={name} className="mb-4">
                  <label htmlFor={name} className="auth-label">
                    {label}:
                  </label>
                  <Field name={name} type={type} className="form-input" id={name} />
                  <ErrorMessage name={name} component="div" className="auth-error" />
                </div>
              ))}

              {/* Checkbox: Autorizado para salir solo */}
              <div className="mb-4 col-span-full">
                <label className="flex items-center space-x-2">
                  <Field name="autorizadoParaSalirSolo" type="checkbox" className="form-checkbox" />
                  <span>Autorizado para salir solo</span>
                </label>
              </div>

              {/* Checkbox: Activo (solo en edición) */}
              {alumnoId !== null && (
                <div className="mb-4 col-span-full">
                  <label className="flex items-center space-x-2">
                    <Field name="activo">
                      {({ field }: any) => (
                        <input
                          type="checkbox"
                          {...field}
                          checked={field.value === true}
                          onChange={(e) =>
                            setFieldValue(field.name, e.target.checked)
                          }
                        />
                      )}
                    </Field>
                    <span>Activo</span>
                  </label>
                </div>
              )}

              {/* Otras Notas */}
              <div className="col-span-full mb-4">
                <label htmlFor="otrasNotas" className="auth-label">
                  Otras Notas:
                </label>
                <Field as="textarea" name="otrasNotas" id="otrasNotas" className="form-input h-24" />
                <ErrorMessage name="otrasNotas" component="div" className="auth-error" />
              </div>
            </div>

            <div className="form-acciones">
              <Button type="submit" disabled={isSubmitting} className="page-button">
                Guardar Alumno
              </Button>
              <Button
                type="button"
                onClick={() => {
                  resetearFormulario();
                  Object.keys(initialAlumnoValues).forEach((key) => {
                    setFieldValue(
                      key,
                      initialAlumnoValues[key as keyof (AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>)]
                    );
                  });
                }}
                className="page-button-group flex justify-end mb-4"
              >
                Limpiar
              </Button>
              <Button type="button" onClick={() => navigate("/alumnos")} className="page-button-group flex justify-end mb-4">
                Volver al Listado
              </Button>
            </div>

            {mensaje && (
              <p className={`form-mensaje ${mensaje.includes("correctamente") ? "form-mensaje-success" : "form-mensaje-error"}`}>
                {mensaje}
              </p>
            )}

            {/* Inscripciones del Alumno */}
            <fieldset className="form-fieldset mt-8">
              <legend className="form-legend text-xl font-semibold">
                Inscripciones del Alumno
              </legend>
              {alumnoId ? (
                <>
                  <div className="page-button-group flex justify-end mb-4">
                    <Boton
                      onClick={() => navigate(`/inscripciones/formulario?alumnoId=${alumnoId}`)}
                      className="page-button"
                    >
                      Agregar Disciplina
                    </Boton>
                  </div>
                  <div className="overflow-x-auto">
                    <Tabla
                      encabezados={[
                        "ID",
                        "Disciplina",
                        "Bonificación",
                        "Costo",
                        "Acciones",
                      ]}
                      datos={inscripciones}
                      extraRender={(fila) => [
                        fila.id,
                        fila.disciplina?.nombre ?? "Sin Disciplina",
                        fila.bonificacion?.descripcion ?? "N/A",
                      ]}
                      acciones={(fila) => (
                        <div className="flex gap-2">
                          <Boton
                            onClick={() => navigate(`/inscripciones/formulario?id=${fila.id}`)}
                            className="page-button-group flex justify-end mb-4"
                          >
                            Editar
                          </Boton>
                          <Boton
                            onClick={() => handleEliminarInscripcion(fila.id)}
                            className="bg-accent text-white hover:bg-accent/90"
                          >
                            Eliminar
                          </Boton>
                        </div>
                      )}
                    />
                  </div>
                </>
              ) : (
                <p className="text-gray-500 text-sm mt-4">
                  No se pueden gestionar inscripciones hasta que <strong>se guarde</strong> un alumno.
                </p>
              )}
            </fieldset>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AlumnosFormulario;
