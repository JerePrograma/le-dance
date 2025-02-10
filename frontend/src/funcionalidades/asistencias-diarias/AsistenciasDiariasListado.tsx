// src/funcionalidades/asistencias-diarias/AsistenciasDiariasListado.tsx
import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2, Eye } from "lucide-react";
import asistenciasApi from "../../api/asistenciasApi";
import type { AsistenciaMensualListadoResponse } from "../../types/types";

const AsistenciasDiariasListado: React.FC = () => {
    // Ahora usamos el tipo de listado mensual, ya que es lo que deseamos mostrar.
    const [asistencias, setAsistencias] = useState<AsistenciaMensualListadoResponse[]>([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const itemsPerPage = 5;
    const navigate = useNavigate();

    const fetchAsistencias = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            // Llamamos al endpoint de asistencias mensuales, no al de asistencias diarias
            const data = await asistenciasApi.listarAsistenciasMensuales();
            setAsistencias(data);
        } catch (error) {
            console.error("Error al cargar asistencias mensuales:", error);
            setError("Error al cargar las asistencias.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchAsistencias();
    }, [fetchAsistencias]);

    const pageCount = useMemo(() => Math.ceil(asistencias.length / itemsPerPage), [asistencias.length]);
    const currentItems = useMemo(
        () => asistencias.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
        [asistencias, currentPage]
    );

    const handlePageClick = useCallback(
        ({ selected }: { selected: number }) => {
            if (selected < pageCount) {
                setCurrentPage(selected);
            }
        },
        [pageCount]
    );

    if (loading) return <div className="text-center py-4">Cargando...</div>;
    if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

    return (
        <div className="page-container">
            <h1 className="page-title">Asistencias Mensuales</h1>
            <div className="page-button-group flex justify-end mb-4">
                <Boton
                    onClick={() => navigate("/asistencias-mensuales/formulario")}
                    className="page-button"
                    aria-label="Registrar nueva asistencia mensual"
                >
                    <PlusCircle className="w-5 h-5 mr-2" />
                    Registrar Nueva Asistencia Mensual
                </Boton>
            </div>
            <div className="page-card">
                <Tabla
                    encabezados={[
                        "ID",
                        "Mes",
                        "Año",
                        "Disciplina",
                        "Profesor",
                        "Acciones",
                    ]}
                    datos={currentItems}
                    acciones={(fila) => (
                        <div className="flex gap-2">
                            <Boton
                                onClick={() => navigate(`/asistencias-mensuales/${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Ver detalles de asistencia mensual ${fila.mes}/${fila.anio}`}
                            >
                                <Eye className="w-4 h-4 mr-2" />
                                Ver
                            </Boton>
                            <Boton
                                onClick={() => navigate(`/asistencias-mensuales/formulario?id=${fila.id}`)}
                                className="page-button-secondary"
                                aria-label={`Editar asistencia mensual ${fila.mes}/${fila.anio}`}
                            >
                                <Pencil className="w-4 h-4 mr-2" />
                                Editar
                            </Boton>
                            <Boton
                                className="page-button-danger"
                                aria-label={`Eliminar asistencia mensual ${fila.mes}/${fila.anio}`}
                            >
                                <Trash2 className="w-4 h-4 mr-2" />
                                Eliminar
                            </Boton>
                        </div>
                    )}
                    extraRender={(fila) => [
                        fila.id,
                        fila.mes,
                        fila.anio,
                        fila.disciplina,
                        fila.profesor,
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

export default AsistenciasDiariasListado;
