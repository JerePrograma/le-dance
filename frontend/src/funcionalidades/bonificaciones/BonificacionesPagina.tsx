import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

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
    [bonificaciones.length, itemsPerPage]
  );
  const currentItems = useMemo(
    () =>
      bonificaciones.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [bonificaciones, currentPage, itemsPerPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  if (loading) return <div>Cargando...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Bonificaciones</h1>
      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/bonificaciones/formulario")}>
          Registrar Nueva Bonificación
        </Boton>
      </div>
      <div className="page-table-container">
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
                secondary
                aria-label={`Editar bonificación ${fila.descripcion}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar bonificación ${fila.descripcion}`}
              >
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
