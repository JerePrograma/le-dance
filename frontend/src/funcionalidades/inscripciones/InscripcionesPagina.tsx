"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import inscripcionesApi from "../../api/inscripcionesApi"
import type { InscripcionResponse } from "../../types/types"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil } from "lucide-react"
import Pagination from "../../componentes/ui/Pagination"
import { toast } from "react-toastify"
import mensualidadesApi from "../../api/mensualidadesApi" // API para generar cuotas

const InscripcionesPagina = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
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

  const handleGenerarCuotas = async () => {
    try {
      const respuestas = await mensualidadesApi.generarMensualidadesParaMesVigente()
      toast.success(`Se generaron/actualizaron ${respuestas.length} cuota(s) para el mes vigente.`)
      fetchInscripciones()
    } catch (error) {
      console.error("Error al generar cuotas:", error)
      toast.error("Error al generar cuotas del mes.")
    }
  }

  // Función para calcular el costo de una inscripción
  const calcularCostoInscripcion = (ins: InscripcionResponse) => {
    const cuota = ins.disciplina?.valorCuota || 0
    const bonifPct = ins.bonificacion?.porcentajeDescuento || 0
    const bonifMonto = ins.bonificacion?.valorFijo || 0
    return cuota - bonifMonto - (cuota * bonifPct) / 100
  }

  // Agrupar inscripciones por alumno
  const gruposInscripciones = useMemo(() => {
    return inscripciones.reduce((acc, ins) => {
      const alumnoId = ins.alumno.id
      if (!acc[alumnoId]) {
        acc[alumnoId] = { alumno: ins.alumno, inscripciones: [] }
      }
      acc[alumnoId].inscripciones.push(ins)
      return acc
    }, {} as Record<number, { alumno: InscripcionResponse["alumno"], inscripciones: InscripcionResponse[] }>)
  }, [inscripciones])

  const gruposArray = useMemo(() => Object.values(gruposInscripciones), [gruposInscripciones])

  // Para cada grupo, calcular el costo total
  const gruposConCosto = useMemo(() => {
    return gruposArray.map((grupo) => {
      const costoTotal = grupo.inscripciones.reduce(
        (sum, ins) => sum + calcularCostoInscripcion(ins),
        0
      )
      return { ...grupo, costoTotal }
    })
  }, [gruposArray])

  // Si hay muchos alumnos, se puede agregar paginación (opcional)
  const itemsPerPage = 5
  const pageCount = useMemo(() => Math.ceil(gruposConCosto.length / itemsPerPage), [gruposConCosto.length])
  const currentPage = useState(0)[0] // Para simplificar, aquí usamos la primera página.
  const currentItems = useMemo(
    () => gruposConCosto.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [gruposConCosto, currentPage, itemsPerPage]
  )

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Inscripciones por Alumno</h1>
        <div className="flex gap-4">
          <Boton
            onClick={handleCrearInscripcion}
            className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
            aria-label="Crear nueva inscripción"
          >
            <PlusCircle className="w-5 h-5" />
            Nueva Inscripción
          </Boton>
          <Boton
            onClick={handleGenerarCuotas}
            className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
            aria-label="Generar cuotas del mes"
          >
            Generar Cuotas del Mes
          </Boton>
        </div>
      </div>

      {gruposConCosto.length === 0 ? (
        <div className="text-center py-4">No hay inscripciones disponibles.</div>
      ) : (
        <div className="overflow-x-auto">
          <Tabla
            encabezados={[
              "ID Alumno",
              "Nombre Alumno",
              "Costo Total",
              "Acciones"
            ]}
            datos={currentItems}
            extraRender={(grupo) => [
              grupo.alumno.id,
              `${grupo.alumno.nombre} ${grupo.alumno.apellido}`,
              grupo.costoTotal.toFixed(2)
            ]}
            acciones={(grupo) => (
              <div className="flex gap-2">
                <Boton
                  onClick={() => navigate(`/inscripciones/formulario?alumnoId=${grupo.alumno.id}`)}
                  className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                >
                  <Pencil className="w-4 h-4" />
                  Ver Detalles
                </Boton>
              </div>
            )}
          />
        </div>
      )}

      {pageCount > 1 && (
        <div className="py-4 border-t">
          <Pagination
            currentPage={currentPage}
            totalPages={pageCount}
            onPageChange={(newPage) => {
              if (newPage >= 0 && newPage < pageCount) {
                // Actualizar el estado de la página, por ejemplo:
                // setCurrentPage(newPage)
              }
            }}
            className="justify-center"
          />
        </div>
      )}
    </div>
  )
}

export default InscripcionesPagina;
