// src/funcionalidades/metodos-pago/MetodosPagoPagina.tsx
import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import metodosPagoApi from "../../api/metodosPagoApi";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import type { MetodoPagoResponse } from "../../types/types";

const itemsPerPage = 5;

const MetodosPagoPagina = () => {
  const [metodos, setMetodos] = useState<MetodoPagoResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const fetchMetodos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await metodosPagoApi.listarMetodosPago();
      setMetodos(response);
    } catch (error) {
      console.error("Error al cargar métodos de pago:", error);
      setError("Error al cargar métodos de pago.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetodos();
  }, [fetchMetodos]);

  const pageCount = useMemo(() => Math.ceil(metodos.length / itemsPerPage), [metodos.length]);
  const currentItems = useMemo(
    () => metodos.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [metodos, currentPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) setCurrentPage(selected);
    },
    [pageCount]
  );

  const handleEliminarMetodo = async (id: number) => {
    try {
      await metodosPagoApi.eliminarMetodoPago(id);
      toast.success("Método de pago eliminado correctamente.");
      fetchMetodos();
    } catch (error) {
      toast.error("Error al eliminar el método de pago.");
    }
  };

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Métodos de Pago</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/metodos-pago/formulario")}
          className="page-button"
          aria-label="Registrar nuevo método de pago"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Método de Pago
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={["ID", "Descripción", "Acciones"]}
          datos={currentItems}
          acciones={(fila: MetodoPagoResponse) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/metodos-pago/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar método de pago ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar método de pago ${fila.descripcion}`}
                onClick={() => handleEliminarMetodo(fila.id)}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila: MetodoPagoResponse) => [fila.id, fila.descripcion]}
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

export default MetodosPagoPagina;
