"use client"

import type React from "react"
import { useEffect, useState, useCallback, useMemo } from "react"
import { useNavigate } from "react-router-dom"
import ReactPaginate from "react-paginate"
import api from "../../api/axiosConfig"
import type { CajaResponse, PageResponse } from "../../types/types"

const CajaPagina: React.FC = () => {
    const [cajas, setCajas] = useState<CajaResponse[]>([])
    const [currentPage, setCurrentPage] = useState(0)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const itemsPerPage = 5
    const navigate = useNavigate()

    const fetchCajas = useCallback(async () => {
        try {
            setLoading(true)
            setError(null)
            const response = await api.get<PageResponse<CajaResponse>>("/cajas")
            setCajas(response.data.content)
        } catch (err) {
            console.error("Error al cargar los registros de caja:", err)
            setError("Error al cargar los registros de caja.")
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        fetchCajas()
    }, [fetchCajas])

    const pageCount = useMemo(() => Math.ceil(cajas.length / itemsPerPage), [cajas.length])

    const currentItems = useMemo(
        () => cajas.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [cajas, currentPage],
    )

    const handlePageClick = useCallback(
        ({ selected }: { selected: number }) => {
            if (selected < pageCount) {
                setCurrentPage(selected)
            }
        },
        [pageCount],
    )

    if (loading) return <div className="flex justify-center items-center p-4 text-gray-600">Cargando registros...</div>

    if (error) return <div className="flex justify-center items-center p-4 text-red-600">{error}</div>

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <h1 className="text-2xl font-bold text-gray-900 mb-6">Caja - Ingresos Diarios</h1>

            <div className="flex justify-end mb-6">
                <button
                    onClick={() => navigate("/caja/formulario")}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors duration-200 flex items-center gap-2"
                    aria-label="Registrar nuevo ingreso"
                >
                    Registrar Nuevo Ingreso
                </button>
            </div>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                {[
                                    "ID",
                                    "Fecha",
                                    "Total Efectivo",
                                    "Total Transferencia",
                                    "Total Tarjeta",
                                    "Rango",
                                    "Observaciones",
                                    "Acciones",
                                ].map((header) => (
                                    <th
                                        key={header}
                                        className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                                    >
                                        {header}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {currentItems.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="px-6 py-4 text-center text-sm text-gray-500 bg-[#FFF5EE]">
                                        No hay transacciones registradas
                                    </td>
                                </tr>
                            ) : (
                                currentItems.map((fila) => (
                                    <tr key={fila.id}>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.id}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.fecha}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.totalEfectivo}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.totalDebito}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.totalTarjeta}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.rangoDesdeHasta}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{fila.observaciones}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                            <div className="flex gap-2">
                                                <button
                                                    onClick={() => navigate(`/caja/formulario?id=${fila.id}`)}
                                                    className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1 rounded-md text-sm transition-colors duration-200"
                                                    aria-label={`Editar ingreso ${fila.id}`}
                                                >
                                                    Editar
                                                </button>
                                                <button
                                                    className="bg-red-100 hover:bg-red-200 text-red-700 px-3 py-1 rounded-md text-sm transition-colors duration-200"
                                                    aria-label={`Eliminar ingreso ${fila.id}`}
                                                >
                                                    Eliminar
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {pageCount > 1 && (
                <div className="mt-4 flex justify-center">
                    <ReactPaginate
                        previousLabel={"← Anterior"}
                        nextLabel={"Siguiente →"}
                        breakLabel={"..."}
                        pageCount={pageCount}
                        onPageChange={handlePageClick}
                        containerClassName={"flex gap-2 items-center"}
                        pageClassName={"inline-flex items-center justify-center w-8 h-8 rounded-md border text-sm"}
                        pageLinkClassName={"w-full h-full flex items-center justify-center"}
                        previousClassName={"inline-flex items-center px-3 py-1 rounded-md border text-sm"}
                        nextClassName={"inline-flex items-center px-3 py-1 rounded-md border text-sm"}
                        breakClassName={"inline-flex items-center justify-center w-8 h-8"}
                        activeClassName={"!bg-blue-600 !text-white !border-blue-600"}
                        disabledClassName={"opacity-50 cursor-not-allowed"}
                    />
                </div>
            )}
        </div>
    )
}

export default CajaPagina

