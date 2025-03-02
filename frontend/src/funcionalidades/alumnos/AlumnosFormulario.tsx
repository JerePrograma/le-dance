import type React from "react";
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Form, Formik, type FormikHelpers } from "formik";
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
import {
  convertToAlumnoRegistroRequest,
  convertToAlumnoModificacionRequest,
} from "../../utilidades/alumnoUtils";
import { Button } from "../../componentes/ui/button";

const initialAlumnoValues: AlumnoRegistroRequest &
  Partial<AlumnoModificacionRequest> = {
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
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoListadoResponse[]
  >([]);
  const [formValues, setFormValues] = useState<
    AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>
  >(initialAlumnoValues);

  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  const handleGuardarAlumno = async (
    values: AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>,
    {
      setSubmitting,
    }: FormikHelpers<AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>>
  ) => {
    try {
      let successMsg: string;
      if (alumnoId) {
        await alumnosApi.actualizar(
          alumnoId,
          convertToAlumnoModificacionRequest(
            values as AlumnoModificacionRequest
          )
        );
        successMsg = "Alumno actualizado correctamente";
      } else {
        const nuevoAlumno = await alumnosApi.registrar(
          values as AlumnoRegistroRequest
        );
        setAlumnoId(nuevoAlumno.id);
        successMsg = "Alumno creado correctamente";
      }
      setMensaje(successMsg);
      toast.success(successMsg);
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message ||
        (error.response?.status === 404
          ? "Alumno no encontrado"
          : "Error al guardar el alumno");
      setMensaje(errorMessage);
      toast.error(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleBuscar = useCallback(async (id: string) => {
    try {
      if (id) {
        const alumno = await alumnosApi.obtenerPorId(Number(id));
        console.log("Alumno data received:", alumno);
        const convertedAlumno = convertToAlumnoRegistroRequest(alumno);
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
  }, []);

  const resetearFormulario = () => {
    setFormValues(initialAlumnoValues);
    setAlumnoId(null);
    setInscripciones([]);
  };

  const handleSeleccionarAlumno = async (
    id: number,
    nombreCompleto: string
  ) => {
    try {
      const alumno = await alumnosApi.obtenerPorId(id);
      setFormValues(convertToAlumnoRegistroRequest(alumno));
      setAlumnoId(alumno.id);
      setNombreBusqueda(nombreCompleto);
      cargarInscripciones(alumno.id);
      setMensaje("");
    } catch (error) {
      setMensaje("Alumno no encontrado.");
      resetearFormulario();
    }
  };

  const cargarInscripciones = useCallback(async (alumnoId: number | null) => {
    if (alumnoId) {
      const inscripcionesDelAlumno = await inscripcionesApi.listar(alumnoId);
      setInscripciones(inscripcionesDelAlumno);
    } else {
      setInscripciones([]);
    }
  }, []);

  useEffect(() => {
    const alumnoIdParam = searchParams.get("alumnoId");
    if (alumnoIdParam) {
      handleBuscar(alumnoIdParam);
    }
  }, [searchParams, handleBuscar]);

  useEffect(() => {
    const buscarSugerencias = async () => {
      if (debouncedNombreBusqueda) {
        const sugerencias = await alumnosApi.buscarPorNombre(
          debouncedNombreBusqueda
        );
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
                  <Boton
                    onClick={() => handleBuscar(idBusqueda)}
                    className="page-button"
                  >
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
              </div>

              {/* Datos Personales */}
              {[
                { name: "nombre", label: "Nombre" },
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
                { name: "celular1", label: "Celular 1" },
                { name: "email1", label: "Email 1", type: "email" },
                { name: "email2", label: "Email 2", type: "email" },
                { name: "documento", label: "Documento" },
                { name: "nombrePadres", label: "Nombre de Padres" },
              ].map(({ name, label, type = "text" }) => (
                <div key={name} className="mb-4">
                  <label htmlFor={name} className="auth-label">
                    {label}:
                  </label>
                  <Field
                    name={name}
                    type={type}
                    className="form-input"
                    id={name}
                  />
                  <ErrorMessage
                    name={name}
                    component="div"
                    className="auth-error"
                  />
                </div>
              ))}

              {/* Checkboxes */}
              {[
                {
                  name: "autorizadoParaSalirSolo",
                  label: "Autorizado para salir solo",
                },
                { name: "activo", label: "Activo" },
              ].map(({ name, label }) => (
                <div key={name} className="mb-4 col-span-full">
                  <label className="flex items-center space-x-2">
                    <Field
                      name={name}
                      type="checkbox"
                      className="form-checkbox"
                    />
                    <span>{label}</span>
                  </label>
                </div>
              ))}

              {/* Otras Notas */}
              <div className="col-span-full mb-4">
                <label htmlFor="otrasNotas" className="auth-label">
                  Otras Notas:
                </label>
                <Field
                  as="textarea"
                  name="otrasNotas"
                  id="otrasNotas"
                  className="form-input h-24"
                />
                <ErrorMessage
                  name="otrasNotas"
                  component="div"
                  className="auth-error"
                />
              </div>
            </div>

            <div className="form-acciones">
              <Button
                type="submit"
                disabled={isSubmitting}
                className="page-button"
              >
                Guardar Alumno
              </Button>
              <Button
                type="button"
                className="page-button-group flex justify-end mb-4"
                onClick={() => {
                  resetearFormulario();
                  Object.keys(initialAlumnoValues).forEach((key) => {
                    setFieldValue(
                      key,
                      initialAlumnoValues[
                      key as keyof (AlumnoRegistroRequest &
                        Partial<AlumnoModificacionRequest>)
                      ]
                    );
                  });
                }}
              >
                Limpiar
              </Button>
              <Button
                type="button"
                className="page-button-group flex justify-end mb-4"
                onClick={() => navigate("/alumnos")}
              >
                Volver al Listado
              </Button>
            </div>

            {mensaje && (
              <p
                className={`form-mensaje ${mensaje.includes("correctamente")
                  ? "form-mensaje-success"
                  : "form-mensaje-error"
                  }`}
              >
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
                      onClick={() =>
                        navigate(
                          `/inscripciones/formulario?alumnoId=${alumnoId}`
                        )
                      }
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
                        "Notas",
                        "Acciones",
                      ]}
                      datos={inscripciones}
                      extraRender={(fila) => [
                        fila.id,
                        fila.disciplina?.nombre ?? "Sin Disciplina",
                        fila.bonificacion?.descripcion ?? "N/A",
                        fila.notas || "-",
                      ]}
                      acciones={(fila) => (
                        <div className="flex gap-2">
                          <Boton
                            onClick={() =>
                              navigate(
                                `/inscripciones/formulario?id=${fila.id}`
                              )
                            }
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
                  No se pueden gestionar inscripciones hasta que{" "}
                  <strong>se guarde</strong> un alumno.
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
