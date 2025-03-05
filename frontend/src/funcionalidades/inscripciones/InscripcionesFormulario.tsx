"use client"

import React, { useEffect, useState, useCallback } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { Formik, Form, Field, ErrorMessage } from "formik"
import { toast } from "react-toastify"
import Boton from "../../componentes/comunes/Boton"

// APIs
import inscripcionesApi from "../../api/inscripcionesApi"
import disciplinasApi from "../../api/disciplinasApi"
import bonificacionesApi from "../../api/bonificacionesApi"

// Types
import type {
  InscripcionRegistroRequest,
  InscripcionResponse,
  BonificacionResponse,
  DisciplinaDetalleResponse,
  InscripcionModificacionRequest,
} from "../../types/types"

// Esquema de validación
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema"

// Extendemos el tipo para el formulario (para distinguir inscripciones ya existentes)
interface InscripcionFormData extends InscripcionRegistroRequest {
  id?: number
}

// Valor inicial para una inscripción nueva
const initialInscripcion: InscripcionFormData = {
  alumnoId: 0,
  inscripcion: {
    disciplinaId: 0,
    bonificacionId: undefined,
  },
  fechaInscripcion: new Date().toISOString().split("T")[0],
}

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  // Listados de catálogos
  const [disciplinas, setDisciplinas] = useState<DisciplinaDetalleResponse[]>([])
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>([])

  // Para el alumno (se asume que viene en la URL)
  const [alumnoId, setAlumnoId] = useState<number>(0)

  // Lista dinámica de inscripciones a agregar/editar (inicialmente vacía)
  const [inscripcionesList, setInscripcionesList] = useState<InscripcionFormData[]>([])

  // Lista de inscripciones previas (ya guardadas) del alumno
  const [prevInscripciones, setPrevInscripciones] = useState<InscripcionResponse[]>([])

  // Cargar catálogos de disciplinas y bonificaciones
  useEffect(() => {
    console.log("Iniciando carga de catálogos de disciplinas y bonificaciones")
    const fetchCatalogos = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ])
        console.log("Catálogos cargados:", { discData, bonData })
        setDisciplinas(discData || [])
        setBonificaciones(bonData || [])
      } catch (error) {
        console.error("Error al cargar catálogos:", error)
        toast.error("Error al cargar disciplinas o bonificaciones.")
      }
    }
    fetchCatalogos()
  }, [])

  // Leer "alumnoId" de la URL y asignarlo a los formularios
  useEffect(() => {
    const alumnoParam = searchParams.get("alumnoId")
    console.log("Parámetro alumnoId desde URL:", alumnoParam)
    if (alumnoParam) {
      const aId = Number(alumnoParam)
      if (!isNaN(aId) && aId !== 0) {
        console.log("AlumnoId válido obtenido:", aId)
        setAlumnoId(aId)
        // Si se agrega un formulario posteriormente, se le asignará el alumnoId
        setInscripcionesList((prev) =>
          prev.map((insc) => ({ ...insc, alumnoId: aId }))
        )
      } else {
        console.warn("AlumnoId no es un número válido o es 0:", aId)
      }
    } else {
      console.warn("No se encontró alumnoId en la URL")
    }
  }, [searchParams])

  // Función para cargar las inscripciones previas del alumno
  const fetchPrevInscripciones = useCallback(async () => {
    if (alumnoId) {
      console.log("Cargando inscripciones previas para alumnoId:", alumnoId)
      try {
        const lista: InscripcionResponse[] = await inscripcionesApi.listar(alumnoId)
        console.log("Inscripciones previas cargadas:", lista)
        setPrevInscripciones(lista)
      } catch (error) {
        console.error("Error al cargar inscripciones previas:", error)
        toast.error("Error al cargar inscripciones previas.")
      }
    } else {
      console.warn("AlumnoId es 0, no se cargarán inscripciones previas")
    }
  }, [alumnoId])

  useEffect(() => {
    fetchPrevInscripciones()
  }, [fetchPrevInscripciones])

  // Para cada inscripción, calculamos los 4 valores:
  // Cuota, Bonificación (%), Bonificación (monto) y Total
  const calcularValores = (ins: InscripcionResponse) => {
    const cuota = ins.disciplina?.valorCuota || 0
    const bonifPct = ins.bonificacion?.porcentajeDescuento || 0
    const bonifMonto = ins.bonificacion?.valorFijo || 0
    const total = cuota - bonifMonto - (cuota * bonifPct) / 100
    return { cuota, bonifPct, bonifMonto, total }
  }

  // Sumas totales
  const totales = prevInscripciones.reduce(
    (acc, ins) => {
      const { cuota, bonifPct, bonifMonto, total } = calcularValores(ins)
      return {
        cuota: acc.cuota + cuota,
        bonifPct: acc.bonifPct + bonifPct,
        bonifMonto: acc.bonifMonto + bonifMonto,
        total: acc.total + total,
      }
    },
    { cuota: 0, bonifPct: 0, bonifMonto: 0, total: 0 }
  )

  // Función para agregar una nueva inscripción (formulario vacío)
  const agregarInscripcion = () => {
    console.log("Agregando nueva inscripción para alumnoId:", alumnoId)
    setInscripcionesList((prev) => [...prev, { ...initialInscripcion, alumnoId }])
  }

  // Función para eliminar un formulario de inscripción (por índice)
  const eliminarInscripcionRow = (index: number) => {
    console.log("Eliminando inscripción del formulario en índice:", index)
    setInscripcionesList((prev) => prev.filter((_, i) => i !== index))
  }

  const handleEliminarInscripcion = async (ins: InscripcionResponse) => {
    console.log("Intentando eliminar inscripción con id:", ins.id)
    try {
      await inscripcionesApi.eliminar(ins.id)
      toast.success("Inscripción eliminada correctamente.")
      setPrevInscripciones((prev) => prev.filter((item) => item.id !== ins.id))
      console.log("Inscripción eliminada correctamente con id:", ins.id)
    } catch (error) {
      console.error("Error al eliminar inscripción:", error)
      toast.error("Error al eliminar inscripción.")
    }
  }

  // Función para "editar" una inscripción previa: la carga en el listado de formularios
  const handleEditarInscripcion = (ins: InscripcionResponse) => {
    console.log("Editando inscripción con id:", ins.id)
    const disciplinaEncontrada = disciplinas.find((d) => d.id === ins.disciplina.id)
    if (!disciplinaEncontrada) {
      toast.error("La inscripción seleccionada no tiene disciplina asignada.")
      console.warn("Disciplina no encontrada para inscripción:", ins)
      return
    }

    const formData: InscripcionFormData = {
      id: ins.id,
      alumnoId: ins.alumno.id,
      inscripcion: {
        disciplinaId: disciplinaEncontrada.id,
        bonificacionId: ins.bonificacion?.id,
      },
      fechaInscripcion:
        ins.fechaInscripcion || new Date().toISOString().split("T")[0],
    }

    console.log("Datos del formulario para editar inscripción:", formData)

    setInscripcionesList((prev) => {
      const idx = prev.findIndex((item) => item.id === formData.id)
      if (idx !== -1) {
        const nuevos = [...prev]
        nuevos[idx] = formData
        console.log("Inscripción editada en el formulario en el índice:", idx)
        return nuevos
      }
      console.log("Agregando inscripción editada al formulario")
      return [...prev, formData]
    })
  }

  // Handler para guardar una inscripción (crear o actualizar)
  const handleGuardarInscripcion = async (
    values: InscripcionFormData,
    _resetForm: () => void
  ) => {
    console.log("Guardando inscripción con valores:", values)
    if (!values.alumnoId || values.alumnoId === 0 || !values.inscripcion.disciplinaId) {
      toast.error("Debes asignar un alumno y una disciplina.")
      console.warn(
        "No se pudo guardar, alumnoId o disciplinaId inválidos:",
        values.alumnoId,
        values.inscripcion.disciplinaId
      )
      return
    }
    try {
      const payload: InscripcionModificacionRequest = {
        alumnoId: values.alumnoId,
        inscripcion: {
          disciplinaId: values.inscripcion.disciplinaId,
          bonificacionId: values.inscripcion.bonificacionId,
        },
      }
      console.log("Payload a enviar:", payload)

      if (values.id) {
        console.log("Actualizando inscripción con id:", values.id)
        await inscripcionesApi.actualizar(values.id, payload)
        toast.success("Inscripción actualizada correctamente.")
        console.log("Inscripción actualizada correctamente:", values.id)
        // Actualizamos el estado local para mantener los cambios en el formulario
        setInscripcionesList((prev) =>
          prev.map((insc) => (insc.id === values.id ? { ...values } : insc))
        )
      } else {
        console.log("Creando nueva inscripción")
        const response = await inscripcionesApi.crear(payload)
        console.log("Respuesta de creación:", response)
        toast.success(
          "Inscripción creada correctamente. La cuota del mes vigente se generó automáticamente."
        )
        setInscripcionesList((prev) => [...prev])
      }

      // Se actualiza el listado de inscripciones previas
      await fetchPrevInscripciones()
    } catch (err) {
      console.error("Error al guardar inscripción:", err)
      toast.error("Error al guardar la inscripción.")
    }
  }

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
                  <th className="px-4 py-2 border border-border">Fecha Inscripción</th>
                  <th className="px-4 py-2 border border-border">Estado Cuota</th>
                  <th className="px-4 py-2 border border-border">Cuota</th>
                  <th className="px-4 py-2 border border-border">Bonificación (%)</th>
                  <th className="px-4 py-2 border border-border">Bonificación (monto)</th>
                  <th className="px-4 py-2 border border-border">Total</th>
                  <th className="px-4 py-2 border border-border">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {prevInscripciones.map((ins) => {
                  const { cuota, bonifPct, bonifMonto, total } = calcularValores(ins)
                  return (
                    <tr key={ins.id} className="border-t border-border">
                      <td className="px-4 py-2 border border-border">{ins.id}</td>
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
                  )
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
            onSubmit={(values, { setSubmitting, resetForm }) => {
              handleGuardarInscripcion(values, resetForm).finally(() =>
                setSubmitting(false)
              )
            }}
          >
            {({ isSubmitting, values, setFieldValue }) => {
              const selectedDiscipline = disciplinas.find(
                (d) => d.id === Number(values.inscripcion.disciplinaId)
              )
              const cuota = selectedDiscipline?.valorCuota ?? 0

              const selectedBonification = bonificaciones.find(
                (b) => b.id === Number(values.inscripcion.bonificacionId)
              )
              const bonificacionPorcentaje =
                selectedBonification?.porcentajeDescuento ?? 0
              const bonificacionValor =
                selectedBonification?.valorFijo ?? 0

              const total =
                cuota - bonificacionValor - (cuota * bonificacionPorcentaje) / 100

              return (
                <Form className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6 border-b pb-4">
                    <div className="space-y-2">
                      <label
                        htmlFor="inscripcion.disciplinaId"
                        className="block text-sm font-medium"
                      >
                        Disciplina
                      </label>
                      <Field
                        as="select"
                        name="inscripcion.disciplinaId"
                        className="w-full px-3 py-2 border border-border rounded-md bg-background"
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                          console.log("Cambio de disciplina:", e.target.value)
                          setFieldValue("inscripcion.disciplinaId", Number(e.target.value))
                        }}
                      >
                        <option value={0}>-- Seleccionar --</option>
                        {disciplinas.map((disc) => (
                          <option key={disc.id} value={disc.id}>
                            {disc.nombre}
                          </option>
                        ))}
                      </Field>
                      <ErrorMessage
                        name="inscripcion.disciplinaId"
                        component="div"
                        className="text-destructive text-sm"
                      />
                    </div>

                    <div className="space-y-2">
                      <label
                        htmlFor="inscripcion.bonificacionId"
                        className="block text-sm font-medium"
                      >
                        Bonificación (Opcional)
                      </label>
                      <Field
                        as="select"
                        name="inscripcion.bonificacionId"
                        className="w-full px-3 py-2 border border-border rounded-md bg-background"
                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                          console.log("Cambio de bonificación:", e.target.value)
                          setFieldValue("inscripcion.bonificacionId", e.target.value ? Number(e.target.value) : undefined)
                        }}
                      >
                        <option value="">-- Ninguna --</option>
                        {bonificaciones.map((bon) => (
                          <option key={bon.id} value={bon.id}>
                            {bon.descripcion}
                          </option>
                        ))}
                      </Field>
                      <ErrorMessage
                        name="inscripcion.bonificacionId"
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
                      <label className="font-medium">Bonificación (monto)</label>
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
                      onClick={() => {
                        console.log("Eliminando formulario de inscripción en índice:", index)
                        eliminarInscripcionRow(index)
                      }}
                      className="inline-flex items-center gap-2 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                    >
                      Eliminar
                    </Boton>
                  </div>
                </Form>
              )
            }}
          </Formik>
        </div>
      ))}

      <div className="flex gap-4">
        <Boton
          onClick={() => {
            console.log("Agregando nuevo formulario de inscripción")
            agregarInscripcion()
          }}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
        >
          Agregar Inscripción
        </Boton>
        <Boton
          onClick={() => {
            console.log("Navegando a formulario del alumno o inscripciones")
            navigate(
              alumnoId
                ? `/alumnos/formulario?id=${alumnoId}`
                : "/inscripciones"
            )
          }}
          className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
        >
          Volver
        </Boton>
      </div>
    </div>
  )
}

export default InscripcionesFormulario
