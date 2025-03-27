"use client";

import React, { useState, useCallback, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../api/axiosConfig";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import { toast } from "react-toastify";
import type { PagoResponse } from "../../types/types";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";

const PaymentList: React.FC = () => {
  const [pagos, setPagos] = useState<PagoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 25; // o 20, según tu necesidad

  const navigate = useNavigate();
  // Usamos un ref para controlar la página actual sin forzar re-render
  const pageRef = useRef(0);

  const fetchPagos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // Obtenemos la página actual del ref
      const currentPage = pageRef.current;
      const response = await api.get<PagoResponse[]>(
        `/pagos?page=${currentPage}&limit=${itemsPerPage}`
      );
      const nuevosPagos = response.data;
      setPagos((prev) => [...prev, ...nuevosPagos]);
      pageRef.current = currentPage + 1;
      if (nuevosPagos.length < itemsPerPage) {
        setHasMore(false);
      }
    } catch (error) {
      toast.error("Error al cargar pagos");
      setError("Error al cargar pagos.");
    } finally {
      setLoading(false);
    }
  }, [itemsPerPage]);

  // Carga inicial de datos
  useEffect(() => {
    fetchPagos();
  }, [fetchPagos]);

  const handleEliminar = async (id: number) => {
    try {
      await api.delete(`/pagos/${id}`);
      // Actualiza la lista eliminando el pago sin necesidad de volver a cargar todos los datos
      setPagos((prev) => prev.filter((pago) => pago.id !== id));
    } catch (error) {
      toast.error("Error al eliminar pago");
    }
  };

  // Mientras no se hayan cargado elementos iniciales, muestra un mensaje
  if (loading && pagos.length === 0) {
    return <div className="text-center py-4">Cargando...</div>;
  }
  if (error && pagos.length === 0) {
    return <div className="text-center py-4 text-destructive">{error}</div>;
  }

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
          headers={[
            "ID",
            "Fecha",
            "Monto",
            "Metodo de Pago",
            "Saldo Restante",
            "Estado",
            "Acciones",
          ]}
          data={pagos}
          customRender={(fila) => [
            fila.id,
            fila.fecha,
            fila.monto,
            fila.metodoPago ? fila.metodoPago.descripcion : "Sin método",
            fila.saldoRestante,
            fila.estadoPago,
          ]}
          actions={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/pagos/editar?id=${fila.id}`)}
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
      <InfiniteScroll
        onLoadMore={fetchPagos}
        hasMore={hasMore}
        loading={loading}
        className="mt-4"
        children={undefined}
      >
        {/* Puedes dejar el contenedor vacío o agregar un mensaje extra */}
      </InfiniteScroll>
    </div>
  );
};

export default PaymentList;
