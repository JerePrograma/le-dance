"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import api from "../../api/axiosConfig"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import type { BonificacionResponse } from "../../types/types"
import Pagination from "../../componentes/comunes/Pagination"
import { toast } from "react-toastify"

const Bonificaciones = () => {
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>([])
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const itemsPerPage = 5
  const navigate = useNavigate()

  const fetchBonificaciones = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get<BonificacionResponse[]>("/bonificaciones")
      setBonificaciones(response.data)
    } catch (error) {
      toast.error("Error al cargar bonificaciones:")
      setError("Error al cargar bonificaciones.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchBonificaciones()
  }, [fetchBonificaciones])

  const pageCount = useMemo(() => Math.ceil(bonificaciones.length / itemsPerPage), [bonificaciones.length])
  const currentItems = useMemo(
    () => bonificaciones.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [bonificaciones, currentPage],
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
      <h1 className="page-title">Bonificaciones</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/bonificaciones/formulario")}
          className="page-button"
          aria-label="Registrar nueva bonificacion"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nueva Bonificacion
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          headers={["ID", "Descripcion", "Descuento (%)", "Descuento (monto)", "Activo", "Acciones"]}
          data={currentItems}
          customRender={(fila) => [
            fila.id,
            fila.descripcion,
            fila.porcentajeDescuento, // Columna de descuento en porcentaje
            fila.valorFijo, // Columna de descuento en monto fijo
            fila.activo ? "Si" : "No",
          ]}
          actions={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/bonificaciones/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar bonificacion ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton className="page-button-danger" aria-label={`Eliminar bonificacion ${fila.descripcion}`}>
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
        />
      </div>
      {pageCount > 1 && (
        <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={handlePageChange} className="mt-4" />
      )}
    </div>
  )
}

export default Bonificaciones

