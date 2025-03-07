"use client"

import { useState, useEffect, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import Tabla from "../../componentes/comunes/Tabla"
import subConceptosApi from "../../api/subConceptosApi"
import Pagination from "../../componentes/comunes/Pagination" // Importamos el nuevo componente de paginación
import Boton from "../../componentes/comunes/Boton"
import { PlusCircle, Pencil, Trash2 } from "lucide-react"
import type { SubConceptoResponse } from "../../types/types"
import { toast } from "react-toastify"

const itemsPerPage = 5

const SubConceptos = () => {
    const [subConceptos, setSubConceptos] = useState<SubConceptoResponse[]>([])
    const [currentPage, setCurrentPage] = useState(0)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const navigate = useNavigate()

    const fetchSubConceptos = useCallback(async () => {
        try {
            setLoading(true)
            setError(null)
            const response = await subConceptosApi.listarSubConceptos()
            setSubConceptos(response)
        } catch (error) {
            toast.error("Error al cargar subconceptos:")
            setError("Error al cargar subconceptos.")
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        fetchSubConceptos()
    }, [fetchSubConceptos])

    const pageCount = useMemo(() => Math.ceil(subConceptos.length / itemsPerPage), [subConceptos.length])
    const currentItems = useMemo(
        () => subConceptos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [subConceptos, currentPage],
    )

    const handlePageChange = useCallback(
        (newPage: number) => {
            if (newPage >= 0 && newPage < pageCount) {
                setCurrentPage(newPage)
            }
        },
        [pageCount],
    )

    const handleEliminarSubConcepto = async (id: number) => {
        try {
            await subConceptosApi.eliminarSubConcepto(id)
            toast.success("Subconcepto eliminado correctamente.")
            fetchSubConceptos()
        } catch (error) {
            toast.error("Error al eliminar el subconcepto.")
        }
    }

    if (loading) return <div className="text-center py-4">Cargando...</div>
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>

    return (
        <div className="page-container">
            <h1 className="page-title">Subconceptos</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/subconceptos/formulario")}
                    className="page-button"
                    aria-label="Registrar nuevo subconcepto"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Nuevo Subconcepto
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    headers={["ID", "Descripcion", "Acciones"]}
                    data={currentItems}
                    actions={(fila: SubConceptoResponse) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/subconceptos/formulario?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar subconcepto ${fila.descripcion}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar subconcepto ${fila.descripcion}`}
                                onClick={() => handleEliminarSubConcepto(fila.id)}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    customRender={(fila: SubConceptoResponse) => [fila.id, fila.descripcion]}
                />
            </div>
            {pageCount > 1 && (
                <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={handlePageChange} className="mt-4" />
            )}
        </div>
    )
}

export default SubConceptos

