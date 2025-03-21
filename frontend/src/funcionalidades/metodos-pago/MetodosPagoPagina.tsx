"use client"

// src/funcionalidades/metodos-pago/MetodosPagoPagina.tsx
import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import metodosPagoApi from "../../api/metodosPagoApi"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import { toast } from "react-toastify"
import type { MetodoPagoResponse } from "../../types/types"
import Pagination from "../../componentes/comunes/Pagination"

const itemsPerPage = 5

const MetodosPagoPagina = () => {
  const [metodos, setMetodos] = useState<MetodoPagoResponse[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const fetchMetodos = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await metodosPagoApi.listarMetodosPago()
      setMetodos(response)
    } catch (error) {
      toast.error("Error al cargar metodos de pago:")
      setError("Error al cargar metodos de pago.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchMetodos()
  }, [fetchMetodos])

  const pageCount = useMemo(() => Math.ceil(metodos.length / itemsPerPage), [metodos.length])
  const currentItems = useMemo(
    () => metodos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [metodos, currentPage],
  )

  const handlePageChange = useCallback(
    (newPage: number) => {
      if (newPage >= 0 && newPage < pageCount) setCurrentPage(newPage)
    },
    [pageCount],
  )

  const handleEliminarMetodo = async (id: number) => {
    try {
      await metodosPagoApi.eliminarMetodoPago(id)
      toast.success("Metodo de pago eliminado correctamente.")
      fetchMetodos()
    } catch (error) {
      toast.error("Error al eliminar el metodo de pago.")
    }
  }

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="page-container">
      <h1 className="page-title">Metodos de Pago</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/metodos-pago/formulario")}
          className="page-button"
          aria-label="Registrar nuevo metodo de pago"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Metodo de Pago
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          headers={["ID", "Descripcion", "Recargo", "Acciones"]}
          data={currentItems}
          actions={(fila: MetodoPagoResponse) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/metodos-pago/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar metodo de pago ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar metodo de pago ${fila.descripcion}`}
                onClick={() => handleEliminarMetodo(fila.id)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          customRender={(fila: MetodoPagoResponse) => [fila.id, fila.descripcion, fila.recargo]}
        />
      </div>
      {pageCount > 1 && (
        <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={handlePageChange} className="mt-4" />
      )}
    </div>
  )
}

export default MetodosPagoPagina

