"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle } from "lucide-react";
import { toast } from "react-toastify";
import type { PagoResponse } from "../../types/types";
import pagosApi from "../../api/pagosApi";

const PaymentListByAlumno: React.FC = () => {
  const { alumnoId } = useParams<{ alumnoId: string }>();
  const navigate = useNavigate();

  const [pagos, setPagos] = useState<PagoResponse[]>([]);
  const [visibleCount, setVisibleCount] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerLoad = 25;

  // Obtener pagos para el alumno
  const fetchPagos = useCallback(async () => {
    if (!alumnoId) return;
    try {
      setLoading(true);
      setError(null);
      const parsedAlumnoId = Number(alumnoId);
      const data = await pagosApi.listarPagosPorAlumno(parsedAlumnoId);
      setPagos(data);
      setVisibleCount(itemsPerLoad);
    } catch (err) {
      toast.error("Error al cargar pagos:");
      setError("Error al cargar pagos.");
    } finally {
      setLoading(false);
    }
  }, [alumnoId, itemsPerLoad]);

  useEffect(() => {
    fetchPagos();
  }, [fetchPagos]);

  const currentItems = useMemo(
    () => pagos.slice(0, visibleCount),
    [pagos, visibleCount]
  );
  const hasMore = visibleCount < pagos.length;

  const loadMore = useCallback(() => {
    setVisibleCount((prev) => Math.min(prev + itemsPerLoad, pagos.length));
  }, [pagos.length, itemsPerLoad]);

  if (loading && pagos.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error && pagos.length === 0)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  // Ordena los pagos de forma descendente
  const sortedPagos = [...currentItems].sort((a, b) => b.id - a.id);

  return (
    <div className="page-container">
      <h1 className="page-title">Pagos del Alumno {alumnoId}</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          type="button"
          onClick={() => navigate("/pagos/formulario")}
          className="page-button-secondary"
        >
          Volver a Pagos
        </Boton>
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
            "Método de Pago",
            "Saldo Restante",
            "Estado",
            "Factura",
          ]}
          data={sortedPagos}
          customRender={(fila) => [
            fila.id,
            fila.fecha,
            fila.monto,
            fila.metodoPago ? fila.metodoPago.descripcion : "Sin método",
            fila.saldoRestante,
            fila.estadoPago,
            // Botón para descargar la factura
            <button
              type="button"
              onClick={() => pagosApi.descargarRecibo(fila.id)}
              className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-sm"
            >
              Descargar Factura
            </button>,
          ]}
        />
      </div>
      <InfiniteScroll
        onLoadMore={loadMore}
        hasMore={hasMore}
        loading={loading}
        className="mt-4"
        children={undefined}
      >
        {/* Mensaje opcional o dejar vacío */}
      </InfiniteScroll>
    </div>
  );
};

export default PaymentListByAlumno;
