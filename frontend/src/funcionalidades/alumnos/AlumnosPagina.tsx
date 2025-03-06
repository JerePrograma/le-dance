"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import alumnosApi from "../../api/alumnosApi"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, CreditCard, Trash2 } from "lucide-react"
import Pagination from "../../componentes/comunes/Pagination"
import { toast } from "react-toastify"

interface AlumnoListado {
  id: number
  nombre: string
  apellido: string
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Estados nuevos para búsqueda y orden
  const [searchTerm, setSearchTerm] = useState("")
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc")
  const itemsPerPage = 5
  const navigate = useNavigate()

  const fetchAlumnos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await alumnosApi.listar()
      setAlumnos(response)
    } catch (error) {
      console.error("Error al cargar alumnos:", error)
      setError("Error al cargar alumnos.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchAlumnos()
  }, [fetchAlumnos])

  // Filtrar y ordenar alumnos
  const alumnosFiltradosYOrdenados = useMemo(() => {
    const filtrados = alumnos.filter((alumno) => {
      const nombreCompleto = `${alumno.nombre} ${alumno.apellido}`.toLowerCase()
      return nombreCompleto.includes(searchTerm.toLowerCase())
    })
    return filtrados.sort((a, b) => {
      const nombreA = `${a.nombre} ${a.apellido}`.toLowerCase()
      const nombreB = `${b.nombre} ${b.apellido}`.toLowerCase()
      if (sortOrder === "asc") return nombreA.localeCompare(nombreB)
      else return nombreB.localeCompare(nombreA)
    })
  }, [alumnos, searchTerm, sortOrder])

  const pageCount = useMemo(() => Math.ceil(alumnosFiltradosYOrdenados.length / itemsPerPage), [alumnosFiltradosYOrdenados.length])
  const currentItems = useMemo(() => {
    return alumnosFiltradosYOrdenados.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage)
  }, [alumnosFiltradosYOrdenados, currentPage, itemsPerPage])

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) {
        setCurrentPage(newPage)
      }
    },
    [pageCount],
  )

  // Generar las opciones para el datalist
  const nombresUnicos = useMemo(() => {
    const nombresSet = new Set(alumnos.map((alumno) => `${alumno.nombre} ${alumno.apellido}`))
    return Array.from(nombresSet)
  }, [alumnos])

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  const eliminarAlumno = async (id: number) => {
    try {
      await alumnosApi.eliminar(id)
      toast.success("Alumno eliminado correctamente.")
      // Vuelve a cargar la lista de alumnos para reflejar el cambio
      fetchAlumnos()
    } catch (error) {
      toast.error("Error al eliminar el Alumno.")
    }
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Alumnos</h1>
        <Boton
          onClick={() => navigate("/alumnos/formulario")}
          className="inline-flex items-center gap-2 bg-primary text-primary-foreground hover:bg-primary/90"
          aria-label="Ficha de Alumnos"
        >
          <PlusCircle className="w-5 h-5" />
          Ficha de Alumnos
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
              setCurrentPage(0) // reiniciar la página
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
          encabezados={["ID", "Nombre", "Apellido", "Acciones"]}
          datos={currentItems}
          extraRender={(fila) => [fila.id, fila.nombre, fila.apellido]}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/alumnos/formulario?id=${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                aria-label={`Editar alumno ${fila.nombre} ${fila.apellido}`}
              >
                <Pencil className="w-4 h-4" />
                Editar
              </Boton>
              <Boton
                onClick={() => navigate(`/cobranza/${fila.id}`)}
                className="inline-flex items-center gap-2 bg-secondary text-secondary-foreground hover:bg-secondary/90"
                aria-label={`Ver cobranza consolidada de ${fila.nombre} ${fila.apellido}`}
              >
                <CreditCard className="w-4 h-4" />
                Cobranza
              </Boton>
              <Boton
                className="inline-flex items-center bg-destructive text-destructive-foreground hover:bg-destructive/90"
                aria-label={`Eliminar alumno ${fila.nombre} ${fila.apellido}`}
                onClick={() => eliminarAlumno(fila.id)}
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

export default Alumnos
