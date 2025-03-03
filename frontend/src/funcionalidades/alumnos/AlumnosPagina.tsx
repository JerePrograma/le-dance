"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import alumnosApi from "../../api/alumnosApi"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, CreditCard } from "lucide-react"
import Pagination from "../../componentes/ui/Pagination"

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

  const pageCount = useMemo(() => Math.ceil(alumnos.length / itemsPerPage), [alumnos.length])
  const currentItems = useMemo(() => {
    return alumnos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage)
  }, [alumnos, currentPage])

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

