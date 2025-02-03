import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Bonificacion {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const Bonificaciones = () => {
  const [bonificaciones, setBonificaciones] = useState<Bonificacion[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchBonificaciones = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get<Bonificacion[]>("/api/bonificaciones");
      setBonificaciones(response.data);
    } catch (error) {
      console.error("Error al cargar bonificaciones:", error);
      setError("Error al cargar bonificaciones.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBonificaciones();
  }, [fetchBonificaciones]);

  const pageCount = useMemo(
    () => Math.ceil(bonificaciones.length / itemsPerPage),
    [bonificaciones.length]
  );
  const currentItems = useMemo(
    () =>
      bonificaciones.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [bonificaciones, currentPage]
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
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Bonificaciones</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/bonificaciones/formulario")}
          className="page-button"
          aria-label="Registrar nueva bonificación"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nueva Bonificación
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={[
            "ID",
            "Descripción",
            "Descuento (%)",
            "Activo",
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/bonificaciones/formulario?id=${fila.id}`)
                }
                className="page-button-secondary"
                aria-label={`Editar bonificación ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar bonificación ${fila.descripcion}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.descripcion,
            fila.porcentajeDescuento,
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

export default Bonificaciones;
