"use client"

import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import tipoStocksApi from "../../api/tipoStocksApi"
import Pagination from "../../componentes/comunes/Pagination" // Importamos el nuevo componente de paginación
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import type { TipoStockResponse } from "../../types/types"
import { toast } from "react-toastify"

const itemsPerPage = 5

const TipoStocks = () => {
    const [tipos, setTipos] = useState<TipoStockResponse[]>([])
    const [currentPage, setCurrentPage] = useState(0)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const navigate = useNavigate()

    const fetchTipos = useCallback(async () => {
        try {
            setLoading(true)
            setError(null)
            const response = await tipoStocksApi.listarTiposStock()
            setTipos(response)
        } catch (error) {
            console.error("Error al cargar tipos de stock:", error)
            setError("Error al cargar tipos de stock.")
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        fetchTipos()
    }, [fetchTipos])

    const pageCount = useMemo(() => Math.ceil(tipos.length / itemsPerPage), [tipos.length])
    const currentItems = useMemo(
        () => tipos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [tipos, currentPage],
    )

    const handlePageChange = useCallback(
        (newPage: number) => {
            if (newPage >= 0 && newPage < pageCount) {
                setCurrentPage(newPage)
            }
        },
        [pageCount],
    )

    const handleEliminarTipo = async (id: number) => {
        try {
            await tipoStocksApi.eliminarTipoStock(id)
            toast.success("Tipo de stock eliminado correctamente.")
            fetchTipos()
        } catch (error) {
            toast.error("Error al eliminar el tipo de stock.")
        }
    }

    if (loading) return <div className="text-center py-4">Cargando...</div>
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>

    return (
        <div className="page-container">
            <h1 className="page-title">Tipos de Stock</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/tipo-stocks/formulario")}
                    className="page-button"
                    aria-label="Registrar nuevo tipo de stock"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Nuevo Tipo de Stock
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    encabezados={["ID", "Descripcion", "Activo", "Acciones"]}
                    datos={currentItems}
                    acciones={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/tipo-stocks/formulario?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar tipo ${fila.descripcion}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar tipo ${fila.descripcion}`}
                                onClick={() => handleEliminarTipo(fila.id)}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    extraRender={(fila) => [fila.id, fila.descripcion, fila.activo ? "Si" : "No"]}
                />
            </div>
            {pageCount > 1 && (
                <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={handlePageChange} className="mt-4" />
            )}
        </div>
    )
}

export default TipoStocks

