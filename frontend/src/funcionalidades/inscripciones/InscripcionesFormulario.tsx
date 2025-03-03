"use client"

import type React from "react"

import { useEffect, useState, useCallback } from "react"
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

  // Lista dinámica de inscripciones a agregar/editar
  const [inscripcionesList, setInscripcionesList] = useState<InscripcionFormData[]>([{ ...initialInscripcion }])

  // Lista de inscripciones previas (ya guardadas) del alumno
  const [prevInscripciones, setPrevInscripciones] = useState<InscripcionResponse[]>([])

  // Cargar catálogos de disciplinas y bonificaciones
  useEffect(() => {
    const fetchCatalogos = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ])
        setDisciplinas(discData || [])
        setBonificaciones(bonData || [])
      } catch (error) {
        toast.error("Error al cargar disciplinas o bonificaciones.")
      }
    }
    fetchCatalogos()
  }, [])

  // Leer "alumnoId" de la URL y asignarlo a los formularios
  useEffect(() => {
    const alumnoParam = searchParams.get("alumnoId")
    if (alumnoParam) {
      const aId = Number(alumnoParam)
      if (!isNaN(aId)) {
        setAlumnoId(aId)
        setInscripcionesList((prev) => prev.map((insc) => ({ ...insc, alumnoId: aId })))
      }
    }
  }, [searchParams])

  // Función para cargar las inscripciones previas del alumno
  const fetchPrevInscripciones = useCallback(async () => {
    if (alumnoId) {
      try {
        const lista: InscripcionResponse[] = await inscripcionesApi.listar(alumnoId)
        setPrevInscripciones(lista)
      } catch (error) {
        toast.error("Error al cargar inscripciones previas.")
      }
    }
  }, [alumnoId])

  useEffect(() => {
    fetchPrevInscripciones()
  }, [fetchPrevInscripciones])

  // Función para agregar una nueva inscripción (formulario vacío)
  const agregarInscripcion = () => {
    setInscripcionesList((prev) => [...prev, { ...initialInscripcion, alumnoId }])
  }

  // Función para eliminar un formulario de inscripción (por índice)
  const eliminarInscripcionRow = (index: number) => {
    setInscripcionesList((prev) => prev.filter((_, i) => i !== index))
  }

  const handleEliminarInscripcion = async (ins: InscripcionResponse) => {
    try {
      await inscripcionesApi.eliminar(ins.id)
      toast.success("Inscripción eliminada correctamente.")
      // Actualiza el estado removiendo la inscripción eliminada
      setPrevInscripciones(prev => prev.filter(item => item.id !== ins.id))
      // O, alternativamente, puedes recargar la lista:
      // fetchPrevInscripciones()
    } catch (error) {
      console.error("Error al eliminar inscripción:", error)
      toast.error("Error al eliminar inscripción.")
    }
  }

  // Función para "editar" una inscripción previa: la carga en el listado de formularios
  const handleEditarInscripcion = (ins: InscripcionResponse) => {
    // Buscamos la disciplina en el catálogo usando el id que trae el API en el objeto disciplina
    const disciplinaEncontrada = disciplinas.find((d) => d.id === ins.disciplina.id)
    if (!disciplinaEncontrada) {
      toast.error("La inscripción seleccionada no tiene disciplina asignada.")
      return
    }

    const formData: InscripcionFormData = {
      id: ins.id,
      alumnoId: ins.alumno.id,
      inscripcion: {
        disciplinaId: disciplinaEncontrada.id,
        bonificacionId: ins.bonificacion?.id,
      },
      fechaInscripcion: ins.fechaInscripcion || new Date().toISOString().split("T")[0],
    }

    // Actualizamos la lista de inscripciones
    setInscripcionesList((prev) => {
      const idx = prev.findIndex((item) => item.id === formData.id)
      if (idx !== -1) {
        const nuevos = [...prev]
        nuevos[idx] = formData
        return nuevos
      }
      return [...prev, formData]
    })
  }

  // Handler para guardar una inscripción (si tiene id, actualizar; sino, crear)
  const handleGuardarInscripcion = async (values: InscripcionFormData, resetForm: () => void) => {
    if (!values.alumnoId || !values.inscripcion.disciplinaId) {
      toast.error("Debes asignar un alumno y una disciplina.")
      return
    }
    try {
      if (values.id) {
        await inscripcionesApi.actualizar(values.id, {
          alumnoId: values.alumnoId,
          disciplinaId: values.inscripcion.disciplinaId,
          bonificacionId: values.inscripcion.bonificacionId,
        })
        toast.success("Inscripción actualizada correctamente.")
      } else {
        await inscripcionesApi.crear(values)
        toast.success("Inscripción creada correctamente.")
      }
      resetForm()
      // También recargamos la lista de inscripciones previas
      fetchPrevInscripciones()
    } catch (err) {
      toast.error("Error al guardar la inscripción.")
    }
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      <h1 className="text-3xl font-bold tracking-tight mb-6">Registrar Inscripciones</h1>

      {/* Listado de inscripciones previas con botón "Editar" */}
      {prevInscripciones.length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-2">Inscripciones Previas</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full border border-border">
              <thead>
                <tr className="bg-muted">
                  <th className="px-4 py-2 border border-border">ID</th>
                  <th className="px-4 py-2 border border-border">Disciplina</th>
                  <th className="px-4 py-2 border border-border">Fecha Inscripción</th>
                  <th className="px-4 py-2 border border-border">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {prevInscripciones.map((ins) => (
                  <tr key={ins.id} className="border-t border-border">
                    <td className="px-4 py-2 border border-border">{ins.id}</td>
                    <td className="px-4 py-2 border border-border">{ins.disciplina?.nombre || "N/A"}</td>
                    <td className="px-4 py-2 border border-border">{ins.fechaInscripcion}</td>
                    <td className="px-4 py-2 border border-border">
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
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Sección de formularios para agregar/editar inscripciones */}
      {inscripcionesList.map((inscripcion, index) => (
        <div key={index} className="border border-border rounded-lg p-6 mb-6 bg-card">
          <Formik
            initialValues={inscripcion}
            validationSchema={inscripcionEsquema}
            onSubmit={(values, { setSubmitting, resetForm }) => {
              handleGuardarInscripcion(values, resetForm).finally(() => {
                setSubmitting(false)
              })
            }}
            enableReinitialize
          >
            {({ isSubmitting, values, setFieldValue }) => {
              const selectedDiscipline = disciplinas.find((d) => d.id === Number(values.inscripcion.disciplinaId))
              const cuota = selectedDiscipline?.valorCuota ?? 0

              const selectedBonification = bonificaciones.find(
                (b) => b.id === Number(values.inscripcion.bonificacionId),
              )
              const bonificacionValor = selectedBonification?.valorFijo ?? 0

              const total = cuota - bonificacionValor

              return (
                <Form className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  {/* Campo oculto para alumnoId */}
                  <Field type="hidden" name="alumnoId" />

                  {/* Seleccionar Disciplina */}
                  <div className="space-y-2">
                    <label htmlFor="inscripcion.disciplinaId" className="block text-sm font-medium">
                      Disciplina:
                    </label>
                    <Field
                      as="select"
                      name="inscripcion.disciplinaId"
                      className="w-full px-3 py-2 border border-border rounded-md bg-background"
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
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

                  {/* Mostrar Cuota (solo lectura) */}
                  <div className="space-y-2">
                    <label className="block text-sm font-medium">Cuota:</label>
                    <input
                      type="number"
                      value={cuota}
                      readOnly
                      className="w-full px-3 py-2 border border-border rounded-md bg-muted text-foreground"
                    />
                  </div>

                  {/* Seleccionar Bonificación */}
                  <div className="space-y-2">
                    <label htmlFor="inscripcion.bonificacionId" className="block text-sm font-medium">
                      Bonificación (Opcional):
                    </label>
                    <Field
                      as="select"
                      name="inscripcion.bonificacionId"
                      className="w-full px-3 py-2 border border-border rounded-md bg-background"
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
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

                  {/* Mostrar Valor de Bonificación (solo lectura) */}
                  <div className="space-y-2">
                    <label className="block text-sm font-medium">Valor Bonificación:</label>
                    <input
                      type="number"
                      value={bonificacionValor}
                      readOnly
                      className="w-full px-3 py-2 border border-border rounded-md bg-muted text-foreground"
                    />
                  </div>

                  {/* Fecha de Inscripción */}
                  <div className="space-y-2">
                    <label htmlFor="fechaInscripcion" className="block text-sm font-medium">
                      Fecha de Inscripción:
                    </label>
                    <Field
                      name="fechaInscripcion"
                      type="date"
                      className="w-full px-3 py-2 border border-border rounded-md bg-background"
                    />
                    <ErrorMessage name="fechaInscripcion" component="div" className="text-destructive text-sm" />
                  </div>

                  {/* Mostrar Total Calculado */}
                  <div className="space-y-2">
                    <label className="block text-sm font-medium">Total (Cuota - Bonificación):</label>
                    <input
                      type="number"
                      value={total}
                      readOnly
                      className="w-full px-3 py-2 border border-border rounded-md bg-muted text-foreground"
                    />
                  </div>

                  {/* Botones para Guardar y Eliminar */}
                  <div className="flex gap-4 col-span-full">
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
              )
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
          onClick={() => navigate(alumnoId ? `/alumnos/formulario?id=${alumnoId}` : "/inscripciones")}
          className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
        >
          Volver
        </Boton>
      </div>
    </div>
  )
}

export default InscripcionesFormulario

