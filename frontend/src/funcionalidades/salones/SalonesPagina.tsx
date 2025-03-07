"use client"

import { useEffect, useState, useCallback } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import salonesApi from "../../api/salonesApi"
import Pagination from "../../componentes/comunes/Pagination" // Importamos el nuevo componente de paginación
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil } from "lucide-react"
import type { SalonResponse, Page } from "../../types/types"

const Salones = () => {
  const [salones, setSalones] = useState<Page<SalonResponse>>({
    content: [],
    totalPages: 0,
    totalElements: 0,
    size: 10,
    number: 0,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const fetchSalones = useCallback(async (page = 0) => {
    try {
      setLoading(true)
      setError(null)
      const response = await salonesApi.listarSalones(page)
      setSalones(response)
    } catch (error) {
      toast.error("Error al cargar salones:", error)
      setError("Error al cargar salones.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSalones()
  }, [fetchSalones])

  const handlePageChange = useCallback(
    (newPage: number) => {
      fetchSalones(newPage)
    },
    [fetchSalones],
  )

  if (loading) return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="page-container">
      <h1 className="page-title">Salones</h1>

      <div className="page-button-group flex justify-end mb-4">
        <Boton onClick={() => navigate("/salones/formulario")} className="page-button" aria-label="Ficha de Salones">
          <PlusCircle className="w-5 h-5 mr-2" />
          Ficha de Salones
        </Boton>
      </div>

      <div className="page-card">
        <Tabla
          headers={["ID", "Nombre", "Descripcion", "Acciones"]}
          data={salones.content}
          actions={(fila) => (
            <Boton
              onClick={() => navigate(`/salones/formulario?id=${fila.id}`)}
              className="page-button-secondary"
              aria-label={`Editar salon ${fila.nombre}`}
            >
              <Pencil className="w-4 h-4 mr-2" />
              Editar
            </Boton>
          )}
          customRender={(fila) => [fila.id, fila.nombre, fila.descripcion || "-"]}
        />
      </div>

      {salones.totalPages > 1 && (
        <Pagination
          currentPage={salones.number}
          totalPages={salones.totalPages}
          onPageChange={handlePageChange}
          className="mt-4"
        />
      )}
    </div>
  )
}

export default Salones

