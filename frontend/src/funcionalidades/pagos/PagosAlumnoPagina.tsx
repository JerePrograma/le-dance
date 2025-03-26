"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import Pagination from "../../componentes/comunes/Pagination";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import type { PagoResponse } from "../../types/types";
import pagosApi from "../../api/pagosApi";

const PaymentListByAlumno: React.FC = () => {
    // Usamos el parámetro "alumnoId" según se definió en la ruta: /pagos/alumno/:alumnoId
    const { alumnoId } = useParams<{ alumnoId: string }>();
    const navigate = useNavigate();

    const [pagos, setPagos] = useState<PagoResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const itemsPerPage = 5;

    // Función para obtener los pagos filtrados por alumno usando el endpoint de pagosApi
    const fetchPagos = useCallback(async () => {
        if (!alumnoId) return;
        try {
            setLoading(true);
            setError(null);
            const parsedAlumnoId = Number(alumnoId);
            const data = await pagosApi.listarPagosPorAlumno(parsedAlumnoId);
            setPagos(data);
        } catch (err) {
            toast.error("Error al cargar pagos:");
            setError("Error al cargar pagos.");
        } finally {
            setLoading(false);
        }
    }, [alumnoId]);

    useEffect(() => {
        fetchPagos();
    }, [fetchPagos]);

    const pageCount = useMemo(() => Math.ceil(pagos.length / itemsPerPage), [pagos.length]);
    const currentItems = useMemo(
        () => pagos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [pagos, currentPage]
    );

    const handlePageChange = useCallback(
        (newPage: number) => {
            if (newPage >= 0 && newPage < pageCount) {
                setCurrentPage(newPage);
            }
        },
        [pageCount]
    );

    const handleEliminar = async (id: number) => {
        try {
            await pagosApi.eliminarPago(id);
            fetchPagos();
        } catch (err) {
            toast.error("Error al eliminar pago:");
        }
    };

    if (loading) return <div className="text-center py-4">Cargando...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Pagos del Alumno {alumnoId}</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/pagos/formulario")}
                    className="page-button"
                    aria-label="Registrar nuevo pago"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Pago
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    headers={[
                        "ID",
                        "Fecha",
                        "Monto",
                        "Método de Pago",
                        "Saldo Restante",
                        "Estado",
                        "Acciones",
                    ]}
                    data={currentItems}
                    customRender={(fila) => [
                        fila.id,
                        fila.fecha,
                        fila.monto,
                        fila.metodoPago ? fila.metodoPago.descripcion : "Sin método",
                        fila.saldoRestante,
                        fila.estadoPago,
                    ]}
                    actions={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/pagos/editar?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar pago ${fila.id}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                onClick={() => handleEliminar(fila.id)}
                                className="page-button-danger"
                                aria-label={`Eliminar pago ${fila.id}`}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                />
            </div>
            {pageCount > 1 && (
                <Pagination
                    currentPage={currentPage}
                    totalPages={pageCount}
                    onPageChange={handlePageChange}
                    className="mt-4"
                />
            )}
        </div>
    );
};

export default PaymentListByAlumno;
