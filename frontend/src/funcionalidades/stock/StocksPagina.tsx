import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import stocksApi from "../../api/stocksApi";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import type { StockResponse } from "../../types/types";
import { toast } from "react-toastify";

const itemsPerPage = 5;

const Stocks = () => {
    const [stocks, setStocks] = useState<StockResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const fetchStocks = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await stocksApi.listarStocks();
            setStocks(response);
        } catch (error) {
            console.error("Error al cargar stocks:", error);
            setError("Error al cargar stocks.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchStocks();
    }, [fetchStocks]);

    const pageCount = useMemo(
        () => Math.ceil(stocks.length / itemsPerPage),
        [stocks.length]
    );

    const currentItems = useMemo(
        () =>
            stocks.slice(
                currentPage * itemsPerPage,
                (currentPage + 1) * itemsPerPage
            ),
        [stocks, currentPage]
    );

    const handlePageClick = useCallback(
        ({ selected }: { selected: number }) => {
            if (selected < pageCount) {
                setCurrentPage(selected);
            }
        },
        [pageCount]
    );

    const handleEliminarStock = async (id: number) => {
        try {
            await stocksApi.eliminarStock(id);
            toast.success("Stock eliminado correctamente.");
            fetchStocks();
        } catch (error) {
            toast.error("Error al eliminar el stock.");
        }
    };

    if (loading) return <div className="text-center py-4">Cargando...</div>;
    if (error)
        return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Stocks</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/stocks/formulario")}
                    className="page-button"
                    aria-label="Registrar nuevo stock"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Nuevo Stock
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    encabezados={[
                        "ID",
                        "Nombre",
                        "Precio",
                        "Tipo",
                        "Stock",
                        "Fecha Ingreso",
                        "Fecha Egreso",
                        "Tipo Egreso",
                        "Activo",
                        "Acciones",
                    ]}
                    datos={currentItems}
                    acciones={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/stocks/formulario?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar stock ${fila.nombre}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar stock ${fila.nombre}`}
                                onClick={() => handleEliminarStock(fila.id)}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    extraRender={(fila) => [
                        fila.id,
                        fila.nombre,
                        fila.precio,
                        fila.tipo.descripcion,
                        fila.stock,
                        fila.fechaIngreso,
                        fila.fechaEgreso || "-",
                        fila.activo ? "Sí" : "No",
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

export default Stocks;
