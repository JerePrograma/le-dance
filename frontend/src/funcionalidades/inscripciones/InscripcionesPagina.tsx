"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import inscripcionesApi from "../../api/inscripcionesApi"
import mensualidadesApi from "../../api/mensualidadesApi"
import type { InscripcionResponse } from "../../types/types"
import Boton from "../../componentes/comunes/Boton"
import { Pencil } from "lucide-react"
import Pagination from "../../componentes/comunes/Pagination"
import { toast } from "react-toastify"
import asistenciasApi from "../../api/asistenciasApi"

const itemsPerPage = 5

const InscripcionesPagina = () => {
  const [inscripciones, setInscripciones] = useState<InscripcionResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [searchTerm, setSearchTerm] = useState("")
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc")
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

  const handleGenerarAsistencias = async () => {
    try {
      // Asegúrate de que la función en inscripcionesApi acepte mes y anio
      await asistenciasApi.crearAsistenciasParaInscripcionesActivas();
      toast.success("Asistencias generadas exitosamente para inscripciones activas");
      fetchInscripciones();
    } catch (error) {
      console.error("Error al generar asistencias:", error);
      toast.error("Error al generar asistencias para inscripciones activas");
    }
  };


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

  const calcularCostoInscripcion = (ins: InscripcionResponse) => {
    const cuota = ins.disciplina?.valorCuota || 0
    const bonifPct = ins.bonificacion?.porcentajeDescuento || 0
    const bonifMonto = ins.bonificacion?.valorFijo || 0
    return cuota - bonifMonto - (cuota * bonifPct) / 100
  }

  // Agrupa las inscripciones por alumno
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

  const gruposConCosto = useMemo(() => {
    return gruposArray.map((grupo) => {
      const costoTotal = grupo.inscripciones.reduce(
        (sum, ins) => sum + calcularCostoInscripcion(ins),
        0
      )
      return { ...grupo, costoTotal }
    })
  }, [gruposArray])

  const gruposFiltradosYOrdenados = useMemo(() => {
    const filtrados = gruposConCosto.filter((grupo) => {
      const nombreCompleto = `${grupo.alumno.nombre} ${grupo.alumno.apellido}`.toLowerCase()
      return nombreCompleto.includes(searchTerm.toLowerCase())
    })
    return filtrados.sort((a, b) => {
      const nombreA = `${a.alumno.nombre} ${a.alumno.apellido}`.toLowerCase()
      const nombreB = `${b.alumno.nombre} ${b.alumno.apellido}`.toLowerCase()
      return sortOrder === "asc" ? nombreA.localeCompare(nombreB) : nombreB.localeCompare(nombreA)
    })
  }, [gruposConCosto, searchTerm, sortOrder])

  const pageCount = useMemo(() => Math.ceil(gruposFiltradosYOrdenados.length / itemsPerPage), [gruposFiltradosYOrdenados.length])
  const currentItems = useMemo(
    () => gruposFiltradosYOrdenados.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [gruposFiltradosYOrdenados, currentPage]
  )

  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(
      gruposConCosto.map((grupo) => `${grupo.alumno.nombre} ${grupo.alumno.apellido}`)
    )
    return Array.from(nombresSet)
  }, [gruposConCosto])

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Inscripciones por Alumno</h1>
        <div className="flex gap-4">
          <Boton
            onClick={handleGenerarAsistencias}
            className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
            aria-label="Generar Asistencias"
          >
            Generar Asistencias
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

      <div className="flex flex-col sm:flex-row sm:justify-between gap-4">
        <div>
          <label htmlFor="search" className="mr-2 font-medium">
            Buscar por nombre:
          </label>
          <input
            id="search"
            list="nombres"
            type="text"
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setCurrentPage(0)
            }}
            placeholder="Escribe o selecciona un nombre..."
            className="border rounded px-2 py-1"
          />
          <datalist id="nombres">
            {nombresUnicos.map((nombre) => (
              <option key={nombre} value={nombre} />
            ))}
          </datalist>
        </div>
        <div>
          <label htmlFor="sortOrder" className="mr-2 font-medium">
            Orden:
          </label>
          <select
            id="sortOrder"
            value={sortOrder}
            onChange={(e) => setSortOrder(e.target.value as "asc" | "desc")}
            className="border rounded px-2 py-1"
          >
            <option value="asc">Ascendente</option>
            <option value="desc">Descendente</option>
          </select>
        </div>
      </div>

      {gruposFiltradosYOrdenados.length === 0 ? (
        <div className="text-center py-4">No hay inscripciones disponibles.</div>
      ) : (
        <div className="overflow-x-auto">
          <Tabla
            encabezados={["ID Alumno", "Nombre Alumno", "Costo Total", "Acciones"]}
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
            onPageChange={setCurrentPage}
            className="justify-center"
          />
        </div>
      )}
    </div>
  )
}

export default InscripcionesPagina
