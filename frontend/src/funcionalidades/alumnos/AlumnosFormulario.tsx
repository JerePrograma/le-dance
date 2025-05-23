"use client";

import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage, type FormikHelpers } from "formik";
import { toast } from "react-toastify";
import alumnosApi from "../../api/alumnosApi";
import inscripcionesApi from "../../api/inscripcionesApi";
import { alumnoEsquema } from "../../validaciones/alumnoEsquema";
import useDebounce from "../../hooks/useDebounce";
import Boton from "../../componentes/comunes/Boton";
import Tabla from "../../componentes/comunes/Tabla";
import { Search, X } from "lucide-react";
import { Button } from "../../componentes/ui/button";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";

// IMPORTACIONES PARA EL FORMULARIO DE INSCRIPCIONES (Modal)
import disciplinasApi from "../../api/disciplinasApi";
import bonificacionesApi from "../../api/bonificacionesApi";
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";

import type {
  AlumnoResponse,
  AlumnoRegistro,
  InscripcionResponse,
  InscripcionRegistroRequest,
  BonificacionResponse,
  DisciplinaDetalleResponse,
} from "../../types/types";

// --- Valores iniciales ---
const today = new Date().toISOString().split("T")[0];

const initialAlumnoValues: AlumnoRegistro = {
  id: 0,
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  fechaIncorporacion: today,
  edad: 0,
  celular1: "",
  celular2: "",
  email: "",
  email2: "",
  documento: "",
  deudaPendiente: false,
  nombrePadres: "",
  autorizadoParaSalirSolo: false,
  activo: true,
  otrasNotas: "",
  cuotaTotal: 0,
};

const initialInscripcion: InscripcionRegistroRequest = {
  alumno: { id: 0, nombre: "", apellido: "" },
  disciplina: {
    id: 0,
    nombre: "",
    salonId: 0,
    profesorId: 0,
    valorCuota: 0,
    matricula: 0,
    horarios: [],
  },
  bonificacionId: undefined,
  fechaInscripcion: new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  }),
};

interface InscripcionesModalProps {
  alumnoId: number;
  editingInscripcion?: InscripcionResponse | null;
  onClose: () => void;
  onInscripcionesChange: (alumnoId: number) => void;
}

const InscripcionesModal: React.FC<InscripcionesModalProps> = ({
  alumnoId,
  onClose,
  onInscripcionesChange,
}) => {
  const [disciplinas, setDisciplinas] = useState<DisciplinaDetalleResponse[]>(
    []
  );
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );
  const [inscripcionesList, setInscripcionesList] = useState<
    InscripcionRegistroRequest[]
  >([]);
  const [prevInscripciones, setPrevInscripciones] = useState<
    InscripcionResponse[]
  >([]);

  // 1) carga catálogos
  useEffect(() => {
    disciplinasApi
      .listarDisciplinas()
      .then(setDisciplinas)
      .catch(console.error);
    bonificacionesApi
      .listarBonificaciones()
      .then(setBonificaciones)
      .catch(console.error);
  }, []);

  // 2) carga previas
  const fetchPrev = useCallback(async () => {
    const lista = await inscripcionesApi.listar(alumnoId);
    setPrevInscripciones(lista);
  }, [alumnoId]);
  useEffect(() => {
    fetchPrev();
  }, [fetchPrev]);

  // 3) handler EDITAR desde la tabla de previas
  const handleEditarPrev = (ins: InscripcionResponse) => {
    // buscamos la disciplina para poblar el formulario
    const d = disciplinas.find((d) => d.id === ins.disciplina.id);
    if (!d) return;
    const formData: InscripcionRegistroRequest = {
      id: ins.id!,
      alumno: {
        id: ins.alumno.id!,
        nombre: ins.alumno.nombre,
        apellido: ins.alumno.apellido,
      },
      disciplina: { ...d, horarios: [] },
      bonificacionId: ins.bonificacion?.id,
      fechaInscripcion: ins.fechaInscripcion,
    };
    // reemplazamos la lista de formularios dejando solo éste
    setInscripcionesList([formData]);
  };

  // 4) handler ELIMINAR desde previas
  const handleEliminarPrev = async (ins: InscripcionResponse) => {
    try {
      await inscripcionesApi.eliminar(ins.id);
      toast.success("Inscripción eliminada correctamente.");
      onInscripcionesChange(alumnoId);
      fetchPrev();
    } catch {
      toast.error("Error al eliminar la inscripción.");
    }
  };

  // 5) handler GUARDAR de cada formulario
  const handleGuardarInscripcion = async (
    values: InscripcionRegistroRequest,
    resetForm: () => void,
    index: number
  ) => {
    const payload = {
      alumno: values.alumno,
      disciplina: values.disciplina,
      bonificacionId: values.bonificacionId,
      fechaInscripcion: values.fechaInscripcion,
    };
    try {
      if ((values as any).id) {
        await inscripcionesApi.actualizar((values as any).id, payload);
      } else {
        await inscripcionesApi.crear(values);
      }
      toast.success("Inscripción guardada correctamente.");
      onInscripcionesChange(alumnoId);
      await fetchPrev();
      resetForm();
      setInscripcionesList((list) => list.filter((_, i) => i !== index));
    } catch {
      toast.error("Error al guardar la inscripción.");
    }
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center z-50">
      <div className="absolute inset-0 bg-background/80" onClick={onClose} />
      <div className="relative bg-card rounded-lg p-6 w-full max-w-4xl shadow-lg border border-border">
        {/* header */}
        {/* listado de previas */}
        {prevInscripciones.length > 0 && (
          <div className="mb-6">
            <h3 className="text-xl font-semibold mb-2">
              Inscripciones Previas
            </h3>
            <Tabla
              headers={[
                "ID",
                "Disciplina",
                "Fecha",
                "Cuota",
                "%",
                "Monto",
                "Total",
                "Acciones",
              ]}
              data={[...prevInscripciones, { _totals: true } as any]}
              customRender={(fila: any) => {
                if (fila._totals) {
                  const sums = prevInscripciones.reduce(
                    (acc, ins) => {
                      const cuota = ins.disciplina.valorCuota;
                      const pct = ins.bonificacion?.porcentajeDescuento || 0;
                      const fijo = ins.bonificacion?.valorFijo || 0;
                      const total = cuota - fijo - (cuota * pct) / 100;
                      return {
                        cuota: acc.cuota + cuota,
                        pct: acc.pct + pct,
                        fijo: acc.fijo + fijo,
                        total: acc.total + total,
                      };
                    },
                    { cuota: 0, pct: 0, fijo: 0, total: 0 }
                  );
                  return [
                    <span key="t" className="font-bold">
                      Totales
                    </span>,
                    "",
                    "",
                    sums.cuota.toFixed(2),
                    sums.pct.toFixed(2),
                    sums.fijo.toFixed(2),
                    sums.total.toFixed(2),
                    "",
                  ];
                }
                const cuota = fila.disciplina.valorCuota;
                const pct = fila.bonificacion?.porcentajeDescuento || 0;
                const fijo = fila.bonificacion?.valorFijo || 0;
                const total = cuota - fijo - (cuota * pct) / 100;
                return [
                  fila.id,
                  fila.disciplina.nombre,
                  fila.fechaInscripcion,
                  cuota.toFixed(2),
                  pct.toFixed(2),
                  fijo.toFixed(2),
                  total.toFixed(2),
                ];
              }}
              actions={(fila: any) =>
                fila._totals ? null : (
                  <div className="flex gap-2">
                    <Boton onClick={() => handleEditarPrev(fila)}>Editar</Boton>
                    <Boton
                      onClick={() => handleEliminarPrev(fila)}
                      className="bg-destructive text-white"
                    >
                      Eliminar
                    </Boton>
                  </div>
                )
              }
            />
          </div>
        )}

        {/* Sección de formularios para agregar/editar inscripciones */}
        {inscripcionesList.map((inscripcion, index) => (
          <div
            key={inscripcion.id ?? index}
            className="border border-border rounded-lg p-6 mb-6 bg-card"
          >
            <Formik
              key={inscripcion.id ?? index}
              initialValues={inscripcion}
              validationSchema={inscripcionEsquema}
              onSubmit={async (values, actions) => {
                await handleGuardarInscripcion(
                  values,
                  actions.resetForm,
                  index
                );
                actions.setSubmitting(false);
              }}
            >
              {({ isSubmitting, values, setFieldValue, errors }) => {
                const selectedDisc = disciplinas.find(
                  (d) => d.id === Number(values.disciplina.id)
                );
                const cuota = selectedDisc?.valorCuota ?? 0;
                const selectedBon = bonificaciones.find(
                  (b) => b.id === Number(values.bonificacionId)
                );
                const bonifPct = selectedBon?.porcentajeDescuento ?? 0;
                const bonifMonto = selectedBon?.valorFijo ?? 0;
                const total = cuota - bonifMonto - (cuota * bonifPct) / 100;

                return (
                  <Form className="space-y-6">
                    {Object.keys(errors).length > 0 && (
                      <div className="text-red-600 text-sm">
                        <pre>{JSON.stringify(errors, null, 2)}</pre>
                      </div>
                    )}

                    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 border-b pb-4">
                      {/* Disciplina */}
                      <div className="space-y-2">
                        <label
                          htmlFor="disciplina.id"
                          className="block text-sm font-medium"
                        >
                          Disciplina
                        </label>
                        <Field
                          as="select"
                          name="disciplina.id"
                          className="w-full px-3 py-2 border border-border rounded-md bg-background"
                          onChange={(
                            e: React.ChangeEvent<HTMLSelectElement>
                          ) => {
                            const selectedId = Number(e.target.value);
                            setFieldValue("disciplina.id", selectedId);
                            const found = disciplinas.find(
                              (d) => d.id === selectedId
                            );
                            if (found) {
                              setFieldValue("disciplina", {
                                id: found.id,
                                nombre: found.nombre,
                                salonId: found.salonId,
                                profesorId: found.profesorId,
                                valorCuota: found.valorCuota,
                                matricula: found.matricula,
                                horarios: [],
                              });
                            } else {
                              setFieldValue("disciplina", {
                                id: 0,
                                nombre: "",
                                salonId: 0,
                                profesorId: 0,
                                valorCuota: 0,
                                matricula: 0,
                                horarios: [],
                              });
                            }
                          }}
                        >
                          <option value={0}>-- Seleccione Disciplina --</option>
                          {disciplinas.map((disc) => (
                            <option key={disc.id} value={disc.id}>
                              {disc.nombre}
                            </option>
                          ))}
                        </Field>
                        <ErrorMessage
                          name="disciplina.id"
                          component="div"
                          className="text-destructive text-sm"
                        />
                      </div>
                      {/* Bonificación */}
                      <div className="space-y-2">
                        <label
                          htmlFor="bonificacionId"
                          className="block text-sm font-medium"
                        >
                          Bonificación (Opcional)
                        </label>
                        <Field
                          as="select"
                          name="bonificacionId"
                          className="w-full px-3 py-2 border border-border rounded-md bg-background"
                          onChange={(e: { target: { value: any } }) =>
                            setFieldValue(
                              "bonificacionId",
                              e.target.value
                                ? Number(e.target.value)
                                : undefined
                            )
                          }
                        >
                          <option value="">-- Ninguna --</option>
                          {bonificaciones.map((b) => (
                            <option key={b.id} value={b.id}>
                              {b.descripcion}
                            </option>
                          ))}
                        </Field>
                        <ErrorMessage
                          name="bonificacionId"
                          component="div"
                          className="text-destructive text-sm"
                        />
                      </div>
                      {/* Fecha */}
                      <div className="space-y-2">
                        <label
                          htmlFor="fechaInscripcion"
                          className="block text-sm font-medium"
                        >
                          Fecha de Inscripción
                        </label>
                        <Field
                          name="fechaInscripcion"
                          type="date"
                          className="w-full px-3 py-2 border border-border rounded-md bg-background"
                        />
                        <ErrorMessage
                          name="fechaInscripcion"
                          component="div"
                          className="text-destructive text-sm"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                      <div className="space-y-1 text-sm">
                        <label className="font-medium">Cuota</label>
                        <input
                          type="number"
                          value={cuota}
                          readOnly
                          className="w-full px-2 py-1 border border-border rounded bg-muted text-foreground text-center"
                        />
                      </div>
                      <div className="space-y-1 text-sm">
                        <label className="font-medium">Bonificación (%)</label>
                        <input
                          type="number"
                          value={bonifPct}
                          readOnly
                          className="w-full px-2 py-1 border border-border rounded bg-muted text-foreground text-center"
                        />
                      </div>
                      <div className="space-y-1 text-sm">
                        <label className="font-medium">
                          Bonificación (monto)
                        </label>
                        <input
                          type="number"
                          value={bonifMonto}
                          readOnly
                          className="w-full px-2 py-1 border border-border rounded bg-muted text-foreground text-center"
                        />
                      </div>
                      <div className="space-y-1 text-sm">
                        <label className="font-medium">Total</label>
                        <input
                          type="number"
                          value={total.toFixed(2)}
                          readOnly
                          className="w-full px-2 py-1 border border-border rounded bg-muted text-foreground text-center"
                        />
                      </div>
                    </div>

                    <div className="flex gap-4 mt-6">
                      <Boton
                        type="submit"
                        disabled={isSubmitting}
                        className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
                      >
                        {values.id ? "Actualizar" : "Guardar"} Inscripción
                      </Boton>
                      <Boton
                        type="button"
                        onClick={() =>
                          setInscripcionesList((list) =>
                            list.filter((_, i) => i !== index)
                          )
                        }
                        className="inline-flex items-center gap-2 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      >
                        Cerrar
                      </Boton>
                    </div>
                  </Form>
                );
              }}
            </Formik>
          </div>
        ))}

        <div className="flex gap-4">
          <Boton
            onClick={() =>
              setInscripcionesList((prev) => [
                ...prev,
                {
                  ...initialInscripcion,
                  alumno: { ...initialInscripcion.alumno, id: alumnoId },
                },
              ])
            }
            className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          >
            Agregar Inscripción
          </Boton>
          <Boton
            onClick={onClose}
            className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
          >
            Volver
          </Boton>
        </div>
      </div>
    </div>
  );
};

// --- Componente Principal: Formulario de Alumno con Modal de Inscripciones ---
const AlumnosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [alumnoId, setAlumnoId] = useState<number | null>(null);
  // Usamos InscripcionResponse[], ya que es lo que devuelve la API
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoResponse[]
  >([]);
  const [activeSuggestionIndex, setActiveSuggestionIndex] =
    useState<number>(-1);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [formValues, setFormValues] =
    useState<AlumnoRegistro>(initialAlumnoValues);
  const [showInscripcionModal, setShowInscripcionModal] = useState(false);
  // justo junto a showInscripcionModal
  const [selectedInscripcion, setSelectedInscripcion] =
    useState<InscripcionResponse | null>(null);

  const searchWrapperRef = useRef<HTMLDivElement>(null);
  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  const calcularEdad = (fecha: string): number => {
    const hoy = new Date();
    const nacimiento = new Date(fecha);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();
    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  };

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

  const cargarInscripciones = useCallback(async (alumnoId: number | null) => {
    if (alumnoId) {
      const inscripcionesDelAlumno = await inscripcionesApi.listar(alumnoId);
      setInscripciones(inscripcionesDelAlumno);
    } else {
      setInscripciones([]);
    }
  }, []);

  // Función para recargar inscripciones y actualizar el listado en el componente principal
  const recargarInscripciones = async (alumnoId: number) => {
    try {
      const listaActualizada = await inscripcionesApi.listar(alumnoId);
      setInscripciones(listaActualizada);
    } catch (error) {
      console.error("Error al recargar inscripciones", error);
    }
  };

  const handleBuscar = useCallback(
    async (id: string) => {
      try {
        if (id) {
          const alumno = await alumnosApi.obtenerPorId(Number(id));
          const alumnoForm: AlumnoRegistro = {
            ...alumno,
            activo: alumno.activo ?? true,
          };
          setFormValues(alumnoForm);
          setIdBusqueda(String(alumno.id));
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
    },
    [cargarInscripciones]
  );

  const handleSeleccionarAlumno = async (
    id: number,
    nombreCompleto: string
  ) => {
    try {
      resetearFormulario();
      setIdBusqueda(String(id));
      const alumno = await alumnosApi.obtenerPorId(id);
      const alumnoForm = {
        ...alumno,
        activo: alumno.activo ?? true,
      };
      setFormValues(alumnoForm);
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
    values: AlumnoRegistro,
    { setSubmitting }: FormikHelpers<AlumnoRegistro>
  ) => {
    try {
      if (!alumnoId) {
        const alumnoDuplicado = sugerenciasAlumnos.find(
          (a) =>
            a.nombre.trim().toLowerCase() ===
              values.nombre.trim().toLowerCase() &&
            a.apellido.trim().toLowerCase() ===
              values.apellido.trim().toLowerCase()
        );
        if (alumnoDuplicado) {
          const mensajeError = "Ya existe un alumno con ese nombre y apellido.";
          setMensaje(mensajeError);
          toast.error(mensajeError);
          setSubmitting(false);
          return;
        }
      }

      if (alumnoId) {
        await alumnosApi.actualizar(alumnoId, values);
        toast.success("Alumno actualizado correctamente");
      } else {
        const nuevoAlumno = await alumnosApi.registrar(values);
        setAlumnoId(nuevoAlumno.id);
        setIdBusqueda(String(nuevoAlumno.id));
      }
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

  const handleEliminarInscripcion = async (ins: InscripcionResponse) => {
    try {
      await inscripcionesApi.eliminar(ins.id);
      toast.success("Inscripción eliminada correctamente.");
      if (alumnoId) {
        await cargarInscripciones(alumnoId);
      }
    } catch (error) {}
  };

  useEffect(() => {
    const alumnoIdParam =
      searchParams.get("alumnoId") || searchParams.get("id");
    if (alumnoIdParam) {
      handleBuscar(alumnoIdParam);
    }
  }, [searchParams, handleBuscar]);

  useEffect(() => {
    const buscarSugerencias = async () => {
      const query = debouncedNombreBusqueda.trim();
      if (query !== "") {
        const sugerencias = await alumnosApi.buscarPorNombre(query);
        setSugerenciasAlumnos(sugerencias);
      } else {
        setSugerenciasAlumnos([]);
      }
      setActiveSuggestionIndex(-1);
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
        if (
          activeSuggestionIndex >= 0 &&
          activeSuggestionIndex < sugerenciasAlumnos.length
        ) {
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
      if (
        searchWrapperRef.current &&
        !searchWrapperRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
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
          {({ isSubmitting, setFieldValue, resetForm, values }) => (
            <Form className="formulario max-w-4xl mx-auto">
              {/* Campo oculto */}
              <Field name="id" type="hidden" />
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
                        if (!alumnoId) setIdBusqueda(e.target.value);
                      }}
                      className="form-input flex-grow"
                      readOnly={alumnoId !== null}
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
                {/* Búsqueda por Nombre */}
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
                        const value = e.target.value;
                        setNombreBusqueda(value);
                        setShowSuggestions(value.trim() !== "");
                      }}
                      onFocus={() => {
                        if (nombreBusqueda.trim() !== "") {
                          setShowSuggestions(true);
                        }
                      }}
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
                            className={`sugerencia-item p-2 cursor-pointer ${
                              index === activeSuggestionIndex
                                ? "bg-[hsl(var(--muted))]"
                                : ""
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
                    <ErrorMessage
                      name={name}
                      component="div"
                      className="auth-error"
                    />
                  </div>
                ))}
                <div className="mb-4">
                  <label htmlFor="fechaNacimiento" className="auth-label">
                    Fecha de Nacimiento:
                  </label>
                  <Field
                    name="fechaNacimiento"
                    type="date"
                    className="form-input"
                    id="fechaNacimiento"
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                      setFieldValue("fechaNacimiento", e.target.value);
                    }}
                  />
                  <ErrorMessage
                    name="fechaNacimiento"
                    component="div"
                    className="auth-error"
                  />
                  {values.fechaNacimiento && (
                    <div className="text-sm mt-1">
                      Edad: {calcularEdad(values.fechaNacimiento)} años
                    </div>
                  )}
                </div>
                <div className="mb-4">
                  <label htmlFor="fechaIncorporacion" className="auth-label">
                    Fecha de Incorporación:
                  </label>
                  <Field
                    name="fechaIncorporacion"
                    type="date"
                    className="form-input"
                    id="fechaIncorporacion"
                  />
                  <ErrorMessage
                    name="fechaIncorporacion"
                    component="div"
                    className="auth-error"
                  />
                </div>
                {[
                  { name: "celular1", label: "Celular 1" },
                  { name: "celular2", label: "Celular 2" },
                  { name: "email", label: "Email", type: "email" },
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
                <div className="mb-4 col-span-full">
                  <label className="flex items-center space-x-2">
                    <Field
                      name="autorizadoParaSalirSolo"
                      type="checkbox"
                      className="form-checkbox"
                    />
                    <span>Autorizado para salir solo</span>
                  </label>
                </div>
                {alumnoId !== null && (
                  <div className="mb-4 col-span-full">
                    <label className="flex items-center space-x-2">
                      <Field name="activo">
                        {({ field }: { field: any }) => (
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
              <div className="form-acciones flex gap-4">
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="page-button"
                >
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
                  className={`form-mensaje ${
                    mensaje.includes("correctamente")
                      ? "form-mensaje-success"
                      : "form-mensaje-error"
                  }`}
                >
                  {mensaje}
                </p>
              )}
              <fieldset className="form-fieldset mt-8">
                <legend className="form-legend text-xl font-semibold">
                  Inscripciones del Alumno
                </legend>
                {alumnoId ? (
                  <>
                    <div className="page-button-group flex justify-end mb-4">
                      <Boton
                        onClick={() => {
                          setSelectedInscripcion(null);
                          setShowInscripcionModal(true);
                        }}
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
                        ]}
                        data={[...inscripciones, { _totals: true } as any]}
                        customRender={(fila: any) => {
                          if (fila._totals) {
                            const totales = inscripciones.reduce(
                              (acc, ins) => {
                                const cuota = ins.disciplina?.valorCuota || 0;
                                const pct =
                                  ins.bonificacion?.porcentajeDescuento || 0;
                                const fijo = ins.bonificacion?.valorFijo || 0;
                                const total =
                                  cuota - fijo - (cuota * pct) / 100;
                                return {
                                  cuota: acc.cuota + cuota,
                                  pct: acc.pct + pct,
                                  fijo: acc.fijo + fijo,
                                  total: acc.total + total,
                                };
                              },
                              { cuota: 0, pct: 0, fijo: 0, total: 0 }
                            );
                            return [
                              <span
                                key="totales"
                                className="font-bold text-center"
                              >
                                Totales
                              </span>,
                              "",
                              totales.cuota.toFixed(2),
                              totales.pct.toFixed(2),
                              totales.fijo.toFixed(2),
                              totales.total.toFixed(2),
                            ];
                          } else {
                            const cuota = fila.disciplina?.valorCuota || 0;
                            const pct =
                              fila.bonificacion?.porcentajeDescuento || 0;
                            const fijo = fila.bonificacion?.valorFijo || 0;
                            const total = cuota - fijo - (cuota * pct) / 100;
                            return [
                              fila.id,
                              fila.disciplina?.nombre ?? "Sin Disciplina",
                              cuota.toFixed(2),
                              pct.toFixed(2),
                              fijo.toFixed(2),
                              total.toFixed(2),
                            ];
                          }
                        }}
                        actions={(fila: any) => {
                          if (fila._totals) return null;
                          return (
                            <div className="flex gap-2">
                              <Boton
                                onClick={() => {
                                  setSelectedInscripcion(fila);
                                  setShowInscripcionModal(true);
                                }}
                                className="page-button"
                              >
                                Editar
                              </Boton>
                              <Boton
                                onClick={() => handleEliminarInscripcion(fila)}
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
                    No se pueden gestionar inscripciones hasta que{" "}
                    <strong>se guarde</strong> un alumno.
                  </p>
                )}
              </fieldset>
            </Form>
          )}
        </Formik>
      </div>
      {/* Modal de Inscripciones */}
      {showInscripcionModal && alumnoId && (
        <InscripcionesModal
          alumnoId={alumnoId}
          editingInscripcion={selectedInscripcion} // <-- aquí
          onClose={() => {
            setShowInscripcionModal(false);
            setSelectedInscripcion(null);
          }}
          onInscripcionesChange={recargarInscripciones}
        />
      )}
    </ResponsiveContainer>
  );
};

export default AlumnosFormulario;
