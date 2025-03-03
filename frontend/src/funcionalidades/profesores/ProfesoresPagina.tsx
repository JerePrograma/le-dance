"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import profesoresApi from "../../api/profesoresApi"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import type { ProfesorListadoResponse } from "../../types/types"
import { toast } from "react-toastify"
import Pagination from "../../componentes/ui/Pagination"

const itemsPerPage = 5

const Profesores = () => {
  const [profesores, setProfesores] = useState<ProfesorListadoResponse[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Estados nuevos para búsqueda y orden
  const [searchTerm, setSearchTerm] = useState("")
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc")
  const navigate = useNavigate()

  const fetchProfesores = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await profesoresApi.listarProfesores()
      setProfesores(response)
    } catch (error) {
      console.error("Error al cargar profesores:", error)
      setError("Error al cargar profesores.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchProfesores()
  }, [fetchProfesores])

  // Filtrar y ordenar profesores
  const profesoresFiltradosYOrdenados = useMemo(() => {
    const filtrados = profesores.filter((profesor) => {
      const nombreCompleto = `${profesor.nombre} ${profesor.apellido}`.toLowerCase()
      return nombreCompleto.includes(searchTerm.toLowerCase())
    })
    return filtrados.sort((a, b) => {
      const nombreA = `${a.nombre} ${a.apellido}`.toLowerCase()
      const nombreB = `${b.nombre} ${b.apellido}`.toLowerCase()
      if (sortOrder === "asc") return nombreA.localeCompare(nombreB)
      return nombreB.localeCompare(nombreA)
    })
  }, [profesores, searchTerm, sortOrder])

  const pageCount = useMemo(() => Math.ceil(profesoresFiltradosYOrdenados.length / itemsPerPage), [profesoresFiltradosYOrdenados.length])

  const currentItems = useMemo(
    () =>
      profesoresFiltradosYOrdenados.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage,
      ),
    [profesoresFiltradosYOrdenados, currentPage],
  )

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) {
        setCurrentPage(newPage)
      }
    },
    [pageCount],
  )

  const handleEliminarProfesor = async (id: number) => {
    try {
      await profesoresApi.eliminarProfesor(id)
      toast.success("Profesor eliminado correctamente.")
      fetchProfesores()
    } catch (error) {
      toast.error("Error al eliminar el profesor.")
    }
  }

  // Opciones únicas para el datalist (nombres completos)
  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(
      profesores.map((profesor) => `${profesor.nombre} ${profesor.apellido}`),
    )
    return Array.from(nombresSet)
  }, [profesores])

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Profesores</h1>
        <Boton
          onClick={() => navigate("/profesores/formulario")}
          className="inline-flex items-center"
          aria-label="Registrar nuevo profesor"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Profesor
        </Boton>
      </div>

      {/* Controles de búsqueda y orden */}
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

      <div className="rounded-lg border bg-card text-card-foreground shadow-sm">
        <Tabla
          encabezados={["ID", "Nombre", "Apellido", "Acciones", "Activo"]}
          datos={currentItems}
          extraRender={(fila) => [fila.id, fila.nombre, fila.apellido, fila.activo ? "Si" : "No"]}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/profesores/formulario?id=${fila.id}`)}
                className="inline-flex items-center"
                aria-label={`Editar profesor ${fila.nombre} ${fila.apellido}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="inline-flex items-center bg-destructive text-destructive-foreground hover:bg-destructive/90"
                aria-label={`Eliminar profesor ${fila.nombre} ${fila.apellido}`}
                onClick={() => handleEliminarProfesor(fila.id)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
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

export default Profesores
