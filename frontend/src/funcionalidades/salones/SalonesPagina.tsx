"use client"

import { useEffect, useState, useCallback } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import salonesApi from "../../api/salonesApi"
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll"
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil } from "lucide-react"
import type { SalonResponse, Page } from "../../types/types"
import { toast } from "react-toastify"

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

  // Función que carga una página de salones
  const fetchSalones = useCallback(async (page = 0) => {
    try {
      setLoading(true)
      setError(null)
      const response = await salonesApi.listarSalones(page)
      setSalones((prevSalones) =>
        page === 0
          ? response
          : {
              ...response,
              // Concatena los nuevos resultados con los ya cargados
              content: [...prevSalones.content, ...response.content],
            },
      )
    } catch (error) {
      toast.error("Error al cargar salones:")
      setError("Error al cargar salones.")
    } finally {
      setLoading(false)
    }
  }, [])

  // Carga inicial
  useEffect(() => {
    fetchSalones(0)
  }, [fetchSalones])

  // Función para cargar la siguiente página
  const onLoadMore = useCallback(() => {
    if (salones.number < salones.totalPages - 1) {
      fetchSalones(salones.number + 1)
    }
  }, [fetchSalones, salones.number, salones.totalPages])

  // Determina si hay más páginas para cargar
  const hasMore = salones.number < salones.totalPages - 1

  if (loading && salones.content.length === 0)
    return <div className="text-center py-4">Cargando...</div>
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>

  return (
    <div className="page-container">
      <h1 className="page-title">Salones</h1>

      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/salones/formulario")}
          className="page-button"
          aria-label="Ficha de Salones"
        >
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
              aria-label={`Editar salón ${fila.nombre}`}
            >
              <Pencil className="w-4 h-4 mr-2" />
              Editar
            </Boton>
          )}
          customRender={(fila) => [fila.id, fila.nombre, fila.descripcion || "-"]}
        />
      </div>

      {/* Se muestra el componente de Infinite Scroll solo si hay más páginas */}
      {hasMore && (
        <ListaConInfiniteScroll
          onLoadMore={onLoadMore}
          hasMore={hasMore}
          loading={loading}
          className="mt-4"
        >
          {/* Opcional: se puede incluir un indicador o mensaje */}
          {loading && <div className="text-center py-2">Cargando más...</div>}
        </ListaConInfiniteScroll>
      )}
    </div>
  )
}

export default Salones
