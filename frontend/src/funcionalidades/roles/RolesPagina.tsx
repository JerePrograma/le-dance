"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import api from "../../api/axiosConfig"
import Pagination from "../../componentes/comunes/Pagination" // Importamos el nuevo componente de paginación
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import { toast } from "react-toastify"

interface Rol {
  id: number
  descripcion: string
}

const RolesPagina = () => {
  const [roles, setRoles] = useState<Rol[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const itemsPerPage = 5
  const navigate = useNavigate()

  const fetchRoles = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get<Rol[]>("/roles")
      setRoles(response.data)
    } catch (error) {
      toast.error("Error al cargar roles:")
      setError("Error al cargar roles.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchRoles()
  }, [fetchRoles])

  const pageCount = useMemo(() => Math.ceil(roles.length / itemsPerPage), [roles.length])
  const currentItems = useMemo(
    () => roles.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [roles, currentPage],
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
    <div className="page-container">
      <h1 className="page-title">Roles</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton onClick={() => navigate("/roles/formulario")} className="page-button" aria-label="Registrar Nuevo Rol">
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Rol
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          headers={["ID", "Descripcion", "Acciones"]}
          data={currentItems}
          actions={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/roles/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar rol ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton className="page-button-danger" aria-label={`Eliminar rol ${fila.descripcion}`}>
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          customRender={(fila) => [fila.id, fila.descripcion]}
        />
      </div>
      {pageCount > 1 && (
        <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={handlePageChange} className="mt-4" />
      )}
    </div>
  )
}

export default RolesPagina

