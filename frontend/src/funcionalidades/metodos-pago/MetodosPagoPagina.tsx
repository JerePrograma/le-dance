"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import metodosPagoApi from "../../api/metodosPagoApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import type { MetodoPagoResponse } from "../../types/types";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

const itemsPerPage = 25;

const MetodosPagoPagina = () => {
  const [metodos, setMetodos] = useState<MetodoPagoResponse[]>([]);
  const [visibleCount, setVisibleCount] = useState(itemsPerPage);
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
      toast.error("Error al cargar métodos de pago:");
      setError("Error al cargar métodos de pago.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetodos();
  }, [fetchMetodos]);

  // Se muestra un subconjunto de la lista completa
  const currentItems = useMemo(
    () => metodos.slice(0, visibleCount),
    [metodos, visibleCount]
  );

  // Indica si hay más elementos para cargar
  const hasMore = useMemo(
    () => visibleCount < metodos.length,
    [visibleCount, metodos.length]
  );

  // Incrementa la cantidad visible en bloques
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + itemsPerPage);
    }
  }, [hasMore]);

  const handleEliminarMetodo = async (id: number) => {
    try {
      await metodosPagoApi.eliminarMetodoPago(id);
      toast.success("Método de pago eliminado correctamente.");
      fetchMetodos();
    } catch (error) {
      toast.error("Error al eliminar el método de pago.");
    }
  };

  if (loading && metodos.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

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
          headers={["ID", "Descripción", "Recargo", "Acciones"]}
          data={currentItems}
          actions={(fila: MetodoPagoResponse) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/metodos-pago/formulario?id=${fila.id}`)
                }
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
          customRender={(fila: MetodoPagoResponse) => [
            fila.id,
            fila.descripcion,
            fila.recargo,
          ]}
        />
      </div>
      {hasMore && (
        <ListaConInfiniteScroll
          onLoadMore={onLoadMore}
          hasMore={hasMore}
          loading={loading}
          className="mt-4"
        >
          {loading && <div className="text-center py-2">Cargando más...</div>}
        </ListaConInfiniteScroll>
      )}
    </div>
  );
};

export default MetodosPagoPagina;
