import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import api from "../../api/axiosConfig";
import type { CajaResponse } from "../../types/types";
import type { PageResponse } from "../../types/types"; // Asegúrate de tener definido este tipo

const CajaPagina: React.FC = () => {
    const [cajas, setCajas] = useState<CajaResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const itemsPerPage = 5;
    const navigate = useNavigate();

    const fetchCajas = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            // Supongamos que la API devuelve un objeto paginado
            const response = await api.get<PageResponse<CajaResponse>>("/api/cajas");
            setCajas(response.data.content); // Extraemos la propiedad 'content'
        } catch (err) {
            console.error("Error al cargar los registros de caja:", err);
            setError("Error al cargar los registros de caja.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchCajas();
    }, [fetchCajas]);

    const pageCount = useMemo(() => Math.ceil(cajas.length / itemsPerPage), [cajas.length]);
    const currentItems = useMemo(
        () => cajas.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [cajas, currentPage, itemsPerPage]
    );

    const handlePageClick = useCallback(({ selected }: { selected: number }) => {
        if (selected < pageCount) {
            setCurrentPage(selected);
        }
    }, [pageCount]);

    if (loading) return <div className="text-center py-4">Cargando registros...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Caja - Ingresos Diarios</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/caja/formulario")}
                    className="page-button"
                    aria-label="Registrar nuevo ingreso"
                >
                    Registrar Nuevo Ingreso
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    encabezados={[
                        "ID",
                        "Fecha",
                        "Total Efectivo",
                        "Total Transferencia",
                        "Total Tarjeta",
                        "Rango",
                        "Observaciones",
                    ]}
                    datos={currentItems}
                    acciones={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/caja/formulario?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar ingreso ${fila.id}`}
                            >
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar ingreso ${fila.id}`}
                            >
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    extraRender={(fila) => [
                        fila.id,
                        fila.fecha,
                        fila.totalEfectivo,
                        fila.totalTransferencia,
                        fila.totalTarjeta,
                        fila.rangoDesdeHasta,
                        fila.observaciones,
                    ]}
                />
            </div>
            {pageCount > 1 && (
                <ReactPaginate
                    previousLabel={"← Anterior"}
                    nextLabel={"Siguiente →"}
                    breakLabel={"..."}
                    pageCount={pageCount}
                    onPageChange={handlePageClick}
                    containerClassName={"pagination"}
                    activeClassName={"active"}
                    disabledClassName={"disabled"}
                />
            )}
        </div>
    );
};

export default CajaPagina;
