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

  const pageCount = useMemo(() => Math.ceil(disciplinas.length / itemsPerPage), [disciplinas.length])

  const currentItems = useMemo(
    () => disciplinas.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [disciplinas, currentPage],
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

      <div className="rounded-lg border bg-card text-card-foreground shadow-sm">
        <Tabla
          encabezados={["ID", "Nombre", "Horario", "Acciones"]}
          datos={currentItems}
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
          extraRender={(fila) => [fila.id, fila.nombre, fila.horario]}
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

