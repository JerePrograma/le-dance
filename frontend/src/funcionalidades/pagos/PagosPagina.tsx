import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../api/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Pago {
    id: number;
    fecha: string;
    fechaVencimiento: string;
    monto: number;
    metodoPago: string;
    saldoRestante: number;
    saldoAFavor: number;
    estadoPago: string;
    activo: boolean;
    inscripcionId: number;
}

const PaymentList: React.FC = () => {
    const [pagos, setPagos] = useState<Pago[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const itemsPerPage = 5;
    const navigate = useNavigate();

    const fetchPagos = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.get<Pago[]>("/api/pagos");
            setPagos(response.data);
        } catch (error) {
            console.error("Error al cargar pagos:", error);
            setError("Error al cargar pagos.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchPagos();
    }, [fetchPagos]);

    const pageCount = useMemo(() => Math.ceil(pagos.length / itemsPerPage), [pagos.length]);
    const currentItems = useMemo(
        () => pagos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [pagos, currentPage]
    );

    const handlePageClick = useCallback(({ selected }: { selected: number }) => {
        if (selected < pageCount) {
            setCurrentPage(selected);
        }
    }, [pageCount]);

    const handleEliminar = async (id: number) => {
        try {
            await api.delete(`/api/pagos/${id}`);
            // Actualizamos la lista después de "eliminar" (marcar como inactivo)
            fetchPagos();
        } catch (error) {
            console.error("Error al eliminar pago:", error);
        }
    };

    if (loading) return <div className="text-center py-4">Cargando...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Pagos</h1>
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
                    encabezados={["ID", "Fecha", "Monto", "Método de Pago", "Saldo Restante", "Estado", "Acciones"]}
                    datos={currentItems}
                    extraRender={(fila) => [
                        fila.id,
                        fila.fecha,
                        fila.monto,
                        fila.metodoPago,
                        fila.saldoRestante,
                        fila.estadoPago,
                    ]}
                    acciones={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/pagos/formulario?id=${fila.id}`)}
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

export default PaymentList;
