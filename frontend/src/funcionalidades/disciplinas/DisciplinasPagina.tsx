"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import api from "../../api/axiosConfig"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import Pagination from "../../componentes/ui/Pagination"

interface Disciplina {
  id: number
  nombre: string
  horario: string
}

const Disciplinas = () => {
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Estados para búsqueda y orden
  const [searchTerm, setSearchTerm] = useState("")
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc")
  const itemsPerPage = 5
  const navigate = useNavigate()

  const fetchDisciplinas = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get<Disciplina[]>("/disciplinas")
      setDisciplinas(response.data)
    } catch (error) {
      console.error("Error al cargar disciplinas:", error)
      setError("Error al cargar disciplinas.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDisciplinas()
  }, [fetchDisciplinas])

  // Filtrar y ordenar las disciplinas según el nombre
  const disciplinasFiltradasYOrdenadas = useMemo(() => {
    const filtradas = disciplinas.filter((disciplina) =>
      disciplina.nombre.toLowerCase().includes(searchTerm.toLowerCase())
    )
    return filtradas.sort((a, b) => {
      const nombreA = a.nombre.toLowerCase()
      const nombreB = b.nombre.toLowerCase()
      return sortOrder === "asc" ? nombreA.localeCompare(nombreB) : nombreB.localeCompare(nombreA)
    })
  }, [disciplinas, searchTerm, sortOrder])

  const pageCount = useMemo(() => Math.ceil(disciplinasFiltradasYOrdenadas.length / itemsPerPage), [disciplinasFiltradasYOrdenadas.length])
  const currentItems = useMemo(
    () => disciplinasFiltradasYOrdenadas.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [disciplinasFiltradasYOrdenadas, currentPage, itemsPerPage]
  )

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) {
        setCurrentPage(newPage)
      }
    },
    [pageCount]
  )

  // Opciones únicas para el datalist a partir de los nombres
  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(disciplinas.map((disciplina) => disciplina.nombre))
    return Array.from(nombresSet)
  }, [disciplinas])

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Disciplinas</h1>
        <Boton
          onClick={() => navigate("/disciplinas/formulario")}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          aria-label="Registrar nueva disciplina"
        >
          <PlusCircle className="w-5 h-5" />
          Registrar Nueva Disciplina
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
          encabezados={["ID", "Nombre", "Horario", "Acciones"]}
          datos={currentItems}
          extraRender={(fila) => [fila.id, fila.nombre, fila.horario]}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/disciplinas/formulario?id=${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                aria-label={`Editar disciplina ${fila.nombre}`}
              >
                <Pencil className="w-4 h-4" />
                Editar
              </Boton>
              <Boton
                className="inline-flex items-center gap-2 bg-destructive text-destructive-foreground hover:bg-destructive/90"
                aria-label={`Eliminar disciplina ${fila.nombre}`}
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

export default Disciplinas
