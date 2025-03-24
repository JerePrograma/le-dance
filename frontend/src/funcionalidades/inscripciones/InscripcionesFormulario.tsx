"use client";

import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";

// APIs
import inscripcionesApi from "../../api/inscripcionesApi";
import disciplinasApi from "../../api/disciplinasApi";
import bonificacionesApi from "../../api/bonificacionesApi";

// Types
import type {
  InscripcionResponse,
  BonificacionResponse,
  DisciplinaDetalleResponse,
  InscripcionRegistroRequest,
} from "../../types/types";

// Esquema de validación
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";

// Extendemos el tipo para el formulario (para distinguir inscripciones ya existentes)
interface InscripcionFormData extends InscripcionRegistroRequest {
  id?: number;
}

// Función para obtener la fecha en formato YYYY-MM-DD con GMT-3
const obtenerFechaGTM3 = (): string => {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  });
};

// Valor inicial para una inscripción nueva
const initialInscripcion: InscripcionFormData = {
  alumno: {
    id: 0,
    nombre: "",
    apellido: "",
  },
  // Se asigna una disciplina "vacía"; ignoramos horarios asignando un arreglo vacío
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
  fechaInscripcion: obtenerFechaGTM3(),
};

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Listados de catálogos
  const [disciplinas, setDisciplinas] = useState<DisciplinaDetalleResponse[]>(
    []
  );
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );

  // Guardamos el id del alumno obtenido de la URL
  const [alumnoId, setAlumnoId] = useState<number>(0);

  // Lista dinámica de inscripciones a agregar/editar (inicialmente vacía)
  const [inscripcionesList, setInscripcionesList] = useState<
    InscripcionFormData[]
  >([]);
  // Lista de inscripciones previas (ya guardadas) del alumno
  const [prevInscripciones, setPrevInscripciones] = useState<
    InscripcionResponse[]
  >([]);

  // Cargar catálogos de disciplinas y bonificaciones
  useEffect(() => {
    const fetchCatalogos = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ]);
        setDisciplinas(discData || []);
        setBonificaciones(bonData || []);
      } catch (error) {
        toast.error("Error al cargar disciplinas o bonificaciones.");
      }
    };
    fetchCatalogos();
  }, []);

  // Leer "alumnoId" de la URL y asignarlo al objeto alumno de cada inscripción
  useEffect(() => {
    const alumnoParam = searchParams.get("alumnoId");
    if (alumnoParam) {
      const aId = Number(alumnoParam);
      if (!isNaN(aId) && aId !== 0) {
        setAlumnoId(aId);
        // Actualizamos el alumno.id en los formularios existentes
        setInscripcionesList((prev) =>
          prev.map((insc) => ({
            ...insc,
            alumno: { ...insc.alumno, id: aId },
          }))
        );
      } else {
        toast.warn("AlumnoId no es un número válido o es 0");
      }
    } else {
      toast.warn("No se encontró alumnoId en la URL");
    }
  }, [searchParams]);

  // Agregar un formulario vacío para una nueva inscripción
  const agregarInscripcion = () => {
    setInscripcionesList((prev) => [
      ...prev,
      {
        ...initialInscripcion,
        alumno: { ...initialInscripcion.alumno, id: alumnoId },
      },
    ]);
  };

  // Función para cargar las inscripciones previas del alumno
  const fetchPrevInscripciones = useCallback(async () => {
    if (alumnoId) {
      try {
        const lista: InscripcionResponse[] = await inscripcionesApi.listar(
          alumnoId
        );
        setPrevInscripciones(lista);
      } catch (error) {
        toast.error("Error al cargar inscripciones previas.");
      }
    } else {
      toast.warn("AlumnoId es 0, no se cargarán inscripciones previas");
    }
  }, [alumnoId]);

  useEffect(() => {
    fetchPrevInscripciones();
  }, [fetchPrevInscripciones]);

  // Para cada inscripción, calculamos los valores de cuota, bonificación y total
  const calcularValores = (ins: InscripcionResponse) => {
    const cuota = ins.disciplina?.valorCuota || 0;
    const bonifPct = ins.bonificacion?.porcentajeDescuento || 0;
    const bonifMonto = ins.bonificacion?.valorFijo || 0;
    const total = cuota - bonifMonto - (cuota * bonifPct) / 100;
    return { cuota, bonifPct, bonifMonto, total };
  };

  // Sumas totales de las inscripciones previas
  const totales = prevInscripciones.reduce(
    (acc, ins) => {
      const { cuota, bonifPct, bonifMonto, total } = calcularValores(ins);
      return {
        cuota: acc.cuota + cuota,
        bonifPct: acc.bonifPct + bonifPct,
        bonifMonto: acc.bonifMonto + bonifMonto,
        total: acc.total + total,
      };
    },
    { cuota: 0, bonifPct: 0, bonifMonto: 0, total: 0 }
  );

  // Eliminar un formulario de inscripción (por índice)
  const eliminarInscripcionRow = (index: number) => {
    setInscripcionesList((prev) => prev.filter((_, i) => i !== index));
  };

  const handleEliminarInscripcion = async (ins: InscripcionResponse) => {
    try {
      await inscripcionesApi.eliminar(ins.id);
      toast.success("Inscripción eliminada correctamente.");
      setPrevInscripciones((prev) => prev.filter((item) => item.id !== ins.id));
    } catch (error) {
      toast.error("Error al eliminar inscripción.");
    }
  };

  // Cargar inscripción previa en el formulario para editar
  const handleEditarInscripcion = (ins: InscripcionResponse) => {
    const disciplinaEncontrada = disciplinas.find(
      (d) => d.id === ins.disciplina.id
    );
    if (!disciplinaEncontrada) {
      toast.error("La inscripción seleccionada no tiene disciplina asignada.");
      return;
    }

    // Ignoramos el atributo horarios (asignamos un arreglo vacío)
    const formData: InscripcionFormData = {
      id: ins.id ?? null,
      alumno: {
        id: ins.alumno.id ?? 0,
        nombre: ins.alumno.nombre || "",
        apellido: ins.alumno.apellido || "",
      },
      disciplina: {
        id: disciplinaEncontrada.id,
        nombre: disciplinaEncontrada.nombre,
        salonId: disciplinaEncontrada.salonId,
        profesorId: disciplinaEncontrada.profesorId,
        valorCuota: disciplinaEncontrada.valorCuota,
        matricula: disciplinaEncontrada.matricula,
        horarios: [], // Se ignora el tema de horarios
      },
      bonificacionId: ins.bonificacion?.id ?? null,
      fechaInscripcion: ins.fechaInscripcion || obtenerFechaGTM3(),
    };

    setInscripcionesList((prev) => {
      const idx = prev.findIndex((item) => item.id === formData.id);
      if (idx !== -1) {
        const nuevos = [...prev];
        nuevos[idx] = formData;
        return nuevos;
      }
      return [...prev, formData];
    });
  };

  // Guardar (crear o actualizar) inscripción
  const handleGuardarInscripcion = async (
    values: InscripcionFormData,
    _resetForm: () => void
  ) => {
    if (
      !values.alumno?.id ||
      values.alumno.id === 0 ||
      !values.disciplina?.id ||
      values.disciplina.id === 0
    ) {
      toast.error("Debes asignar un alumno y una disciplina.");
      return;
    }

    try {
      const payload: InscripcionRegistroRequest = {
        alumno: values.alumno,
        disciplina: values.disciplina,
        bonificacionId: values.bonificacionId,
        fechaInscripcion: values.fechaInscripcion,
      };

      if (values.id) {
        await inscripcionesApi.actualizar(values.id, payload);
        toast.success("Inscripción actualizada correctamente.");
        setInscripcionesList((prev) =>
          prev.map((insc) => (insc.id === values.id ? { ...values } : insc))
        );
      } else {
        await inscripcionesApi.crear(values);
        toast.success(
          "Inscripción creada correctamente. La cuota del mes vigente se generó automáticamente."
        );
      }
      await fetchPrevInscripciones();
    } catch (err) {
      toast.error("Error al guardar la inscripción.");
    }
  };

  return (
    <div className="container mx-auto p-6 space-y-8">
      <h1 className="text-3xl font-bold tracking-tight mb-6">
        Registrar Inscripciones
      </h1>

      {/* Listado de inscripciones previas */}
      {prevInscripciones.length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-2">Inscripciones Previas</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full border border-border">
              <thead className="bg-muted">
                <tr>
                  <th className="px-4 py-2 border border-border">ID</th>
                  <th className="px-4 py-2 border border-border">Disciplina</th>
                  <th className="px-4 py-2 border border-border">
                    Fecha Inscripción
                  </th>
                  <th className="px-4 py-2 border border-border">
                    Estado Cuota
                  </th>
                  <th className="px-4 py-2 border border-border">Cuota</th>
                  <th className="px-4 py-2 border border-border">
                    Bonificación (%)
                  </th>
                  <th className="px-4 py-2 border border-border">
                    Bonificación (monto)
                  </th>
                  <th className="px-4 py-2 border border-border">Total</th>
                  <th className="px-4 py-2 border border-border">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {prevInscripciones.map((ins) => {
                  const { cuota, bonifPct, bonifMonto, total } =
                    calcularValores(ins);
                  return (
                    <tr key={ins.id} className="border-t border-border">
                      <td className="px-4 py-2 border border-border">
                        {ins.id}
                      </td>
                      <td className="px-4 py-2 border border-border">
                        {ins.disciplina?.nombre || "N/A"}
                      </td>
                      <td className="px-4 py-2 border border-border">
                        {ins.fechaInscripcion}
                      </td>
                      <td className="px-4 py-2 border border-border">
                        {ins.mensualidadEstado ? (
                          <span className="px-2 py-1 rounded bg-green-200 text-green-800 text-xs">
                            {ins.mensualidadEstado}
                          </span>
                        ) : (
                          "Sin cuota"
                        )}
                      </td>
                      <td className="px-4 py-2 border border-border text-center">
                        {cuota.toFixed(2)}
                      </td>
                      <td className="px-4 py-2 border border-border text-center">
                        {bonifPct.toFixed(2)}
                      </td>
                      <td className="px-4 py-2 border border-border text-center">
                        {bonifMonto.toFixed(2)}
                      </td>
                      <td className="px-4 py-2 border border-border text-center">
                        {total.toFixed(2)}
                      </td>
                      <td className="px-4 py-2 border border-border">
                        <div className="flex gap-2">
                          <Boton
                            onClick={() => handleEditarInscripcion(ins)}
                            className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                          >
                            Editar
                          </Boton>
                          <Boton
                            onClick={() => handleEliminarInscripcion(ins)}
                            className="page-button-danger"
                          >
                            Eliminar
                          </Boton>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot className="bg-muted">
                <tr>
                  <td className="px-4 py-2 border border-border" colSpan={4}>
                    Totales
                  </td>
                  <td className="px-4 py-2 border border-border text-center">
                    {totales.cuota.toFixed(2)}
                  </td>
                  <td className="px-4 py-2 border border-border text-center">
                    {totales.bonifPct.toFixed(2)}
                  </td>
                  <td className="px-4 py-2 border border-border text-center">
                    {totales.bonifMonto.toFixed(2)}
                  </td>
                  <td className="px-4 py-2 border border-border text-center">
                    {totales.total.toFixed(2)}
                  </td>
                  <td className="px-4 py-2 border border-border"></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}

      {/* Sección de formularios para agregar/editar inscripciones */}
      {inscripcionesList.map((inscripcion, index) => (
        <div
          key={inscripcion.id || index}
          className="border border-border rounded-lg p-6 mb-6 bg-card"
        >
          <Formik
            key={inscripcion.id || index}
            initialValues={inscripcion}
            validationSchema={inscripcionEsquema}
            onSubmit={(values, actions) => {
              handleGuardarInscripcion(values, actions.resetForm).finally(
                () => {
                  actions.setSubmitting(false);
                }
              );
            }}
          >
            {({ isSubmitting, values, setFieldValue, errors }) => {
              const selectedDiscipline = disciplinas.find(
                (d) => d.id === Number(values.disciplina.id)
              );
              const cuota = selectedDiscipline?.valorCuota ?? 0;
              const selectedBonification = bonificaciones.find(
                (b) => b.id === Number(values.bonificacionId)
              );
              const bonificacionPorcentaje =
                selectedBonification?.porcentajeDescuento ?? 0;
              const bonificacionValor = selectedBonification?.valorFijo ?? 0;
              const total =
                cuota -
                bonificacionValor -
                (cuota * bonificacionPorcentaje) / 100;

              return (
                <Form className="space-y-6">
                  {Object.keys(errors).length > 0 && (
                    <div className="text-red-600 text-sm">
                      <pre>{JSON.stringify(errors, null, 2)}</pre>
                    </div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6 border-b pb-4">
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
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                          const selectedId = Number(e.target.value);
                          setFieldValue("disciplina.id", selectedId);
                          const found = disciplinas.find(
                            (d) => d.id === selectedId
                          );
                          // Ignoramos horarios asignando arreglo vacío
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
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                          setFieldValue(
                            "bonificacionId",
                            e.target.value ? Number(e.target.value) : undefined
                          )
                        }
                      >
                        <option value="">-- Ninguna --</option>
                        {bonificaciones.map((bon) => (
                          <option key={bon.id} value={bon.id}>
                            {bon.descripcion}
                          </option>
                        ))}
                      </Field>
                      <ErrorMessage
                        name="bonificacionId"
                        component="div"
                        className="text-destructive text-sm"
                      />
                    </div>

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
                        value={bonificacionPorcentaje}
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
                        value={bonificacionValor}
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
                      onClick={() => eliminarInscripcionRow(index)}
                      className="inline-flex items-center gap-2 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                    >
                      Eliminar
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
          onClick={agregarInscripcion}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
        >
          Agregar Inscripción
        </Boton>
        <Boton
          onClick={() =>
            navigate(
              alumnoId ? `/alumnos/formulario?id=${alumnoId}` : "/inscripciones"
            )
          }
          className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
        >
          Volver
        </Boton>
      </div>
    </div>
  );
};

export default InscripcionesFormulario;
