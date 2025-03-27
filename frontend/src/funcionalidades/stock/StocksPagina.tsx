"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import stocksApi from "../../api/stocksApi";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import type { StockResponse } from "../../types/types";
import { toast } from "react-toastify";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

const ITEMS_PER_LOAD = 5;

const Stocks = () => {
  const [stocks, setStocks] = useState<StockResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // visibleCount controla cuántos registros se muestran actualmente
  const [visibleCount, setVisibleCount] = useState<number>(ITEMS_PER_LOAD);
  const navigate = useNavigate();

  const fetchStocks = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await stocksApi.listarStocks();
      setStocks(response);
      setVisibleCount(ITEMS_PER_LOAD);
    } catch (error) {
      toast.error("Error al cargar stocks:");
      setError("Error al cargar stocks.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStocks();
  }, [fetchStocks]);

  // Los elementos que se muestran actualmente
  const currentItems = useMemo(
    () => stocks.slice(0, visibleCount),
    [stocks, visibleCount]
  );

  // Determina si aún quedan elementos por mostrar
  const hasMore = visibleCount < stocks.length;

  // Función para incrementar la cantidad de elementos visibles
  const loadMore = useCallback(() => {
    setVisibleCount((prev) => Math.min(prev + ITEMS_PER_LOAD, stocks.length));
  }, [stocks.length]);

  const handleEliminarStock = async (id: number) => {
    try {
      await stocksApi.eliminarStock(id);
      toast.success("Stock eliminado correctamente.");
      fetchStocks();
    } catch (error) {
      toast.error("Error al eliminar el stock.");
    }
  };

  if (loading && stocks.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error && stocks.length === 0)
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
          headers={[
            "ID",
            "Nombre",
            "Precio",
            "Stock",
            "Fecha Ingreso",
            "Fecha Egreso",
            "Activo",
            "Acciones",
          ]}
          data={currentItems}
          customRender={(fila: StockResponse) => [
            fila.id,
            fila.nombre,
            fila.precio,
            fila.stock,
            fila.fechaIngreso,
            fila.fechaEgreso || "-",
            // Suponemos que aquí se muestra el tipo de egreso o similar; si falta, agrega según tus datos
            fila.activo ? "Si" : "No",
          ]}
          actions={(fila: StockResponse) => (
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
                onClick={() => handleEliminarStock(fila.id)}
                className="page-button-danger"
                aria-label={`Eliminar stock ${fila.nombre}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
        />
      </div>
      {hasMore && (
        <div className="mt-4">
          <ListaConInfiniteScroll
            onLoadMore={loadMore}
            hasMore={hasMore}
            loading={loading}
            className="justify-center"
            children={undefined}
          />
        </div>
      )}
    </div>
  );
};

export default Stocks;
