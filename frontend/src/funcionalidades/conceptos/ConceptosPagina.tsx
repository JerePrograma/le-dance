// src/funcionalidades/conceptos/ConceptosPagina.tsx
import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import conceptosApi from "../../api/conceptosApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from 'lucide-react';
import type { ConceptoResponse } from "../../types/types";
import { toast } from "react-toastify";
import Pagination from "../../componentes/comunes/Pagination";

const itemsPerPage = 5;

const ConceptosPagina = () => {
    const [conceptos, setConceptos] = useState<ConceptoResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const fetchConceptos = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await conceptosApi.listarConceptos();
            setConceptos(response);
        } catch (error) {
            toast.error("Error al cargar conceptos:");
            setError("Error al cargar conceptos.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchConceptos();
    }, [fetchConceptos]);

    const totalPages = useMemo(() => Math.ceil(conceptos.length / itemsPerPage), [conceptos.length]);

    const currentItems = useMemo(
        () => conceptos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [conceptos, currentPage]
    );

    const handlePageChange = useCallback((newPage: number) => {
        setCurrentPage(newPage);
    }, []);

    const handleEliminarConcepto = async (id: number) => {
        try {
            await conceptosApi.eliminarConcepto(id);
            toast.success("Concepto eliminado correctamente.");
            fetchConceptos();
        } catch (error) {
            toast.error("Error al eliminar el concepto.");
        }
    };

    if (loading) return <div className="text-center py-4">Cargando...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Conceptos</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/conceptos/formulario-concepto")}
                    className="page-button"
                    aria-label="Registrar nuevo concepto"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Nuevo Concepto
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    headers={["ID", "Descripcion", "Precio", "SubConcepto", "Acciones"]}
                    data={currentItems}
                    actions={(fila: ConceptoResponse) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/conceptos/formulario-concepto?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar concepto ${fila.descripcion}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar concepto ${fila.descripcion}`}
                                onClick={() => handleEliminarConcepto(fila.id)}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    customRender={(fila: ConceptoResponse) => [
                        fila.id,
                        fila.descripcion,
                        fila.precio,
                        fila.subConcepto.descripcion,
                    ]}
                />
            </div>
            {totalPages > 1 && (
                <div className="mt-4">
                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        onPageChange={handlePageChange}
                        className="justify-center"
                    />
                </div>
            )}
        </div>
    );
};

export default ConceptosPagina;