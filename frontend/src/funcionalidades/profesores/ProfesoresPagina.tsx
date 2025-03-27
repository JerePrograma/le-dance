"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import profesoresApi from "../../api/profesoresApi"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import type { ProfesorListadoResponse } from "../../types/types"
import { toast } from "react-toastify"
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll"

const itemsPerPage = 5

const Profesores = () => {
  const [profesores, setProfesores] = useState<ProfesorListadoResponse[]>([])
  const [visibleCount, setVisibleCount] = useState(itemsPerPage)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Estados para búsqueda y orden
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
      toast.error("Error al cargar profesores:")
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

  // Subconjunto de profesores a mostrar
  const currentItems = useMemo(
    () => profesoresFiltradosYOrdenados.slice(0, visibleCount),
    [profesoresFiltradosYOrdenados, visibleCount],
  )

  // Determina si hay más elementos para cargar
  const hasMore = useMemo(() => visibleCount < profesoresFiltradosYOrdenados.length, [
    visibleCount,
    profesoresFiltradosYOrdenados.length,
  ])

  // Función para cargar más elementos
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + itemsPerPage)
    }
  }, [hasMore])

  // Opciones únicas para el datalist (nombres completos)
  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(
      profesores.map((profesor) => `${profesor.nombre} ${profesor.apellido}`),
    )
    return Array.from(nombresSet)
  }, [profesores])

  // Reinicia la cantidad visible al cambiar el filtro de búsqueda
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value)
    setVisibleCount(itemsPerPage)
  }

  const handleEliminarProfesor = async (id: number) => {
    try {
      await profesoresApi.eliminarProfesor(id)
      toast.success("Profesor eliminado correctamente.")
      fetchProfesores()
    } catch (error) {
      toast.error("Error al eliminar el profesor.")
    }
  }

  if (loading && profesores.length === 0)
    return <div className="text-center py-4">Cargando...</div>
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
            onChange={handleSearchChange}
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
          headers={["ID", "Nombre", "Apellido", "Acciones", "Activo"]}
          data={currentItems}
          customRender={(fila) => [
            fila.id,
            fila.nombre,
            fila.apellido,
            fila.activo ? "Si" : "No",
          ]}
          actions={(fila) => (
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

        {hasMore && (
          <div className="py-4 border-t">
            <ListaConInfiniteScroll
              onLoadMore={onLoadMore}
              hasMore={hasMore}
              loading={loading}
              className="justify-center w-full"
            >
              {loading && <div className="text-center py-2">Cargando más...</div>}
            </ListaConInfiniteScroll>
          </div>
        )}
      </div>
    </div>
  )
}

export default Profesores
