"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import inscripcionesApi from "../../api/inscripcionesApi"
import type { InscripcionResponse } from "../../types/types"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import Pagination from "../../componentes/ui/Pagination"

const InscripcionesPagina = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const itemsPerPage = 5
  const navigate = useNavigate()

  const fetchInscripciones = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await inscripcionesApi.listar()
      setInscripciones(data)
    } catch (error) {
      console.error("Error al cargar inscripciones:", error)
      setError("Error al cargar inscripciones.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchInscripciones()
  }, [fetchInscripciones])

  const handleCrearInscripcion = useCallback(() => {
    navigate("/inscripciones/formulario")
  }, [navigate])

  const handleEliminarInscripcion = useCallback(async (id: number) => {
    try {
      await inscripcionesApi.eliminar(id)
      setInscripciones((prev) => prev.filter((ins) => ins.id !== id))
    } catch (error) {
      console.error("Error al eliminar inscripcion:", error)
    }
  }, [])

  const pageCount = useMemo(() => Math.ceil(inscripciones.length / itemsPerPage), [inscripciones.length])

  const currentItems = useMemo(
    () => inscripciones.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [inscripciones, currentPage],
  )

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) {
        setCurrentPage(newPage)
      }
    },
    [pageCount],
  )

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Inscripciones</h1>
        <Boton
          onClick={handleCrearInscripcion}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          aria-label="Crear nueva inscripcion"
        >
          <PlusCircle className="w-5 h-5" />
          Nueva Inscripción
        </Boton>
      </div>

      <div className="rounded-lg border bg-card text-card-foreground shadow-sm">
        <Tabla
          encabezados={["ID", "Alumno", "Disciplina", "Bonificación", "Descuento (%)", "Descuento (monto)", "Costo", "Acciones"]}
          datos={currentItems}
          extraRender={(fila) => {
            if (!fila) return []
            // Nombre completo del alumno
            const alumnoNombre = fila.alumno ? `${fila.alumno.nombre} ${fila.alumno.apellido}` : "Sin alumno"
            // Nombre de la disciplina
            const disciplinaNombre = fila.disciplina?.nombre || "Sin disciplina"
            // Descripción de la bonificación
            const bonificacionDescripcion = fila.bonificacion ? fila.bonificacion.descripcion : "N/A"
            const bonificacionPorcentaje = fila.bonificacion ? fila.bonificacion.porcentajeDescuento : "N/A"
            const bonificacionMonto = fila.bonificacion ? fila.bonificacion.valorFijo : "N/A"
            // Utilizar el costo calculado desde el backend
            const costoMostrado =
              fila.costoCalculado !== null && fila.costoCalculado !== undefined ? fila.costoCalculado : "0.00"

            return [fila.id ?? "", alumnoNombre, disciplinaNombre, bonificacionDescripcion, bonificacionPorcentaje, bonificacionMonto, costoMostrado]
          }}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/inscripciones/formulario?id=${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
              >
                <Pencil className="w-4 h-4" />
                Editar
              </Boton>
              <Boton
                onClick={() => handleEliminarInscripcion(fila.id)}
                className="inline-flex items-center gap-2 bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                <Trash2 className="w-4 h-4" />
                Eliminar
              </Boton>
            </div>
          )}
        />

        {pageCount > 1 && (
          <div className="py-4 border-t">
            <Pagination
              currentPage={currentPage}
              totalPages={pageCount}
              onPageChange={handlePageChange}
              className="justify-center"
            />
          </div>
        )}
      </div>
    </div>
  )
}

export default InscripcionesPagina

