// src/funcionalidades/alumnos/AlumnosFormulario.tsx
import { useState, useEffect, useCallback, useRef } from "react";
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
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";

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
  inscripciones: [],
  activo: true,
};

const AlumnosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [alumnoId, setAlumnoId] = useState<number | null>(null);
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<AlumnoListadoResponse[]>([]);
  // Estado para el índice de sugerencia activa
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState<number>(-1);
  // Estado para controlar la visibilidad de las sugerencias
  const [showSuggestions, setShowSuggestions] = useState(false);

  const [formValues, setFormValues] = useState<
    AlumnoRegistroRequest & Partial<AlumnoModificacionRequest>
  >(initialAlumnoValues);

  // Ref para detectar clicks fuera del bloque de búsqueda
  const searchWrapperRef = useRef<HTMLDivElement>(null);

  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  // Función para eliminar inscripción y recargar el listado
  const handleEliminarInscripcion = async (id: number) => {
    try {
      await inscripcionesApi.eliminar(id);
      toast.success("Inscripción eliminada correctamente.");
      if (alumnoId) {
        await cargarInscripciones(alumnoId);
      }
    } catch (error) {
      toast.error("Error al eliminar inscripción.");
    }
  };

  // Función para calcular la edad a partir de la fecha de nacimiento
  const calcularEdad = (fecha: string) => {
    const hoy = new Date();
    const nacimiento = new Date(fecha);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();
    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  };

  // Función para resetear formulario y estados relacionados
  const resetearFormulario = () => {
    setFormValues(initialAlumnoValues);
    setAlumnoId(null);
    setInscripciones([]);
    setIdBusqueda("");
    setNombreBusqueda("");
    setSugerenciasAlumnos([]);
    setShowSuggestions(false);
    setSearchParams({});
  };

  // Cargar inscripciones del alumno
  const cargarInscripciones = useCallback(async (alumnoId: number | null) => {
    if (alumnoId) {
      const inscripcionesDelAlumno = await inscripcionesApi.listar(alumnoId);
      setInscripciones(inscripcionesDelAlumno);
    } else {
      setInscripciones([]);
    }
  }, []);

  // Manejar cambio de alumno (buscado por ID)
  const handleBuscar = useCallback(
    async (id: string) => {
      try {
        if (id) {
          const alumno = await alumnosApi.obtenerPorId(Number(id));
          const convertedAlumno = {
            ...convertToAlumnoRegistroRequest(alumno),
            activo: alumno.activo ?? true,
          };
          setFormValues(convertedAlumno);
          setAlumnoId(alumno.id);
          setIdBusqueda(String(alumno.id));
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
    },
    [cargarInscripciones]
  );

  // Manejar selección de alumno desde las sugerencias
  const handleSeleccionarAlumno = async (id: number, nombreCompleto: string) => {
    try {
      resetearFormulario();
      setIdBusqueda(String(id));
      const alumno = await alumnosApi.obtenerPorId(id);
      const convertedAlumno = {
        ...convertToAlumnoRegistroRequest(alumno),
        activo: alumno.activo ?? true,
      };
      setFormValues(convertedAlumno);
      setAlumnoId(alumno.id);
      setNombreBusqueda(nombreCompleto);
      cargarInscripciones(alumno.id);
      setMensaje("");
      setShowSuggestions(false);
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
      if (!alumnoId) {
        const alumnoDuplicado = sugerenciasAlumnos.find(
          (a) =>
            a.nombre.trim().toLowerCase() === values.nombre.trim().toLowerCase() &&
            a.apellido.trim().toLowerCase() === values.apellido.trim().toLowerCase()
        );
        if (alumnoDuplicado) {
          const mensajeError = "Ya existe un alumno con ese nombre y apellido.";
          setMensaje(mensajeError);
          toast.error(mensajeError);
          setSubmitting(false);
          return;
        }
      }

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
        setIdBusqueda(String(nuevoAlumno.id));
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
      if (debouncedNombreBusqueda.trim() === "") {
        const sugerencias = await alumnosApi.buscarPorNombre("");
        setSugerenciasAlumnos(sugerencias);
        setActiveSuggestionIndex(-1);
      } else {
        const sugerencias = await alumnosApi.buscarPorNombre(debouncedNombreBusqueda);
        setSugerenciasAlumnos(sugerencias);
        setActiveSuggestionIndex(-1);
      }
    };
    buscarSugerencias();
  }, [debouncedNombreBusqueda]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (sugerenciasAlumnos.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev < sugerenciasAlumnos.length - 1 ? prev + 1 : 0
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev > 0 ? prev - 1 : sugerenciasAlumnos.length - 1
        );
      } else if (e.key === "Enter" || e.key === "Tab") {
        if (activeSuggestionIndex >= 0 && activeSuggestionIndex < sugerenciasAlumnos.length) {
          e.preventDefault();
          const alumnoSeleccionado = sugerenciasAlumnos[activeSuggestionIndex];
          handleSeleccionarAlumno(
            alumnoSeleccionado.id,
            `${alumnoSeleccionado.nombre} ${alumnoSeleccionado.apellido}`
          );
          setSugerenciasAlumnos([]);
          setActiveSuggestionIndex(-1);
          setShowSuggestions(false);
        }
      }
    }
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchWrapperRef.current && !searchWrapperRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <ResponsiveContainer className="py-4">
      <div className="page-container">
        <h1 className="page-title">Ficha de Alumno</h1>
        <Formik
          initialValues={formValues}
          validationSchema={alumnoEsquema}
          onSubmit={handleGuardarAlumno}
          enableReinitialize
        >
          {({ isSubmitting, setFieldValue, resetForm }) => (
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
                      onChange={(e) => {
                        if (!alumnoId) {
                          setIdBusqueda(e.target.value);
                        }
                      }}
                      className="form-input flex-grow"
                      readOnly={alumnoId !== null}
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
                  <div className="relative" ref={searchWrapperRef}>
                    <input
                      type="text"
                      id="nombreBusqueda"
                      value={nombreBusqueda}
                      onChange={(e) => {
                        setNombreBusqueda(e.target.value);
                        setShowSuggestions(true);
                      }}
                      onFocus={() => setShowSuggestions(true)}
                      onKeyDown={handleKeyDown}
                      className="form-input w-full"
                    />
                    {nombreBusqueda && (
                      <Button
                        type="button"
                        onClick={() => {
                          resetForm();
                          resetearFormulario();
                        }}
                        className="absolute right-2 top-1/2 transform -translate-y-1/2 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]"
                      >
                        Limpiar
                        <X className="w-5 h-5" />
                      </Button>
                    )}
                    {showSuggestions && sugerenciasAlumnos.length > 0 && (
                      <ul className="sugerencias-lista absolute w-full bg-[hsl(var(--popover))] border border-[hsl(var(--border))] z-10">
                        {sugerenciasAlumnos.map((alumno, index) => (
                          <li
                            key={alumno.id}
                            onClick={() =>
                              handleSeleccionarAlumno(
                                alumno.id,
                                `${alumno.nombre} ${alumno.apellido}`
                              )
                            }
                            onMouseEnter={() => setActiveSuggestionIndex(index)}
                            className={`sugerencia-item p-2 cursor-pointer ${index === activeSuggestionIndex ? "bg-[hsl(var(--muted))]" : ""
                              }`}
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
                ].map(({ name, label }) => (
                  <div key={name} className="mb-4">
                    <label htmlFor={name} className="auth-label">
                      {label}:
                    </label>
                    <Field name={name} className="form-input" id={name} />
                    <ErrorMessage name={name} component="div" className="auth-error" />
                  </div>
                ))}

                {/* Fecha de Nacimiento y Edad */}
                <div className="mb-4">
                  <label htmlFor="fechaNacimiento" className="auth-label">
                    Fecha de Nacimiento:
                  </label>
                  <Field name="fechaNacimiento" type="date" className="form-input" id="fechaNacimiento" />
                  <ErrorMessage name="fechaNacimiento" component="div" className="auth-error" />
                  {formValues.fechaNacimiento && (
                    <div className="text-sm mt-1">
                      Edad: {calcularEdad(formValues.fechaNacimiento)} años
                    </div>
                  )}
                </div>

                {/* Fecha de Incorporación */}
                <div className="mb-4">
                  <label htmlFor="fechaIncorporacion" className="auth-label">
                    Fecha de Incorporación:
                  </label>
                  <Field name="fechaIncorporacion" type="date" className="form-input" id="fechaIncorporacion" />
                  <ErrorMessage name="fechaIncorporacion" component="div" className="auth-error" />
                </div>

                {[
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
                        {({ field }: { field: any }) => (
                          <input
                            type="checkbox"
                            {...field}
                            checked={field.value === true}
                            onChange={(e) => setFieldValue(field.name, e.target.checked)}
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
                    resetForm();
                    resetearFormulario();
                  }}
                >
                  Limpiar
                  <X className="w-5 h-5" />
                </Button>
                <Button type="button" onClick={() => navigate("/alumnos")}>
                  Volver al Listado
                </Button>
              </div>

              {mensaje && (
                <p
                  className={`form-mensaje ${mensaje.includes("correctamente") ? "form-mensaje-success" : "form-mensaje-error"
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
                        onClick={() => navigate(`/inscripciones/formulario?alumnoId=${alumnoId}`)}
                        className="page-button"
                      >
                        Agregar Disciplina
                      </Boton>
                    </div>
                    <div className="overflow-x-auto">
                      <Tabla
                        headers={[
                          "ID",
                          "Disciplina",
                          "Cuota",
                          "Bonificación (%)",
                          "Bonificación (monto)",
                          "Total",
                          "Acciones",
                        ]}
                        data={[...inscripciones, { _totals: true } as any]}
                        customRender={(fila: any) => {
                          if (fila._totals) {
                            return [
                              <span key="totales" className="font-bold text-center">
                                Totales
                              </span>,
                              "",
                              inscripciones
                                .reduce((sum, ins) => sum + (ins.disciplina?.valorCuota || 0), 0)
                                .toFixed(2),
                              inscripciones
                                .reduce((sum, ins) => sum + (ins.bonificacion?.porcentajeDescuento || 0), 0)
                                .toFixed(2),
                              inscripciones
                                .reduce((sum, ins) => sum + (ins.bonificacion?.valorFijo || 0), 0)
                                .toFixed(2),
                              inscripciones
                                .reduce((sum, ins) => {
                                  const cuota = ins.disciplina?.valorCuota || 0;
                                  const bonifPct = ins.bonificacion?.porcentajeDescuento || 0;
                                  const bonifMonto = ins.bonificacion?.valorFijo || 0;
                                  return sum + (cuota - bonifMonto - (cuota * bonifPct) / 100);
                                }, 0)
                                .toFixed(2),
                              "",
                            ];
                          } else {
                            const cuota = fila.disciplina?.valorCuota || 0;
                            const bonifPct = fila.bonificacion?.porcentajeDescuento || 0;
                            const bonifMonto = fila.bonificacion?.valorFijo || 0;
                            const total = cuota - bonifMonto - (cuota * bonifPct) / 100;
                            return [
                              fila.id,
                              fila.disciplina?.nombre ?? "Sin Disciplina",
                              cuota.toFixed(2),
                              bonifPct.toFixed(2),
                              bonifMonto.toFixed(2),
                              total.toFixed(2),
                            ];
                          }
                        }}
                        actions={(fila: any) => {
                          if (fila._totals) return null;
                          return (
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
                          );
                        }}
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
    </ResponsiveContainer>
  );
};

export default AlumnosFormulario;
