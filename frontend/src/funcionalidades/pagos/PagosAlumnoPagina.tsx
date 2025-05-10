"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import type { DetallePagoResponse } from "../../types/types";
import detallesPagoApi from "../../api/detallesPagoApi";
import pagosApi from "../../api/pagosApi";

const DetallePagoListByAlumno: React.FC = () => {
  const { alumnoId } = useParams<{ alumnoId: string }>();
  const navigate = useNavigate();

  const [detalles, setDetalles] = useState<DetallePagoResponse[]>([]);
  const [visibleCount, setVisibleCount] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerLoad = 25;

  // Función auxiliar para formatear la fecha en formato dd-mm-aaaa
  const formatDateArgentino = (dateString: string): string => {
    if (!dateString) return "";
    const [year, month, day] = dateString.split("-");
    return `${day}-${month}-${year}`;
  };

  const fetchDetalles = useCallback(async () => {
    if (!alumnoId) return;
    try {
      setLoading(true);
      setError(null);
      const parsedAlumnoId = Number(alumnoId);
      const data = await detallesPagoApi.listarPorAlumno(parsedAlumnoId);
      setDetalles(data);
      setVisibleCount(itemsPerLoad);
    } catch (err) {
      toast.error("Error al cargar detalles de pago.");
      setError("Error al cargar detalles de pago.");
    } finally {
      setLoading(false);
    }
  }, [alumnoId, itemsPerLoad]);

  useEffect(() => {
    fetchDetalles();
  }, [fetchDetalles]);

  const currentItems = useMemo(
    () => detalles.slice(0, visibleCount),
    [detalles, visibleCount]
  );
  const hasMore = visibleCount < detalles.length;
  const loadMore = useCallback(() => {
    setVisibleCount((prev) => Math.min(prev + itemsPerLoad, detalles.length));
  }, [detalles.length, itemsPerLoad]);

  const sortedDetalles = useMemo(
    () => [...currentItems].sort((a, b) => Number(b.pagoId) - Number(a.pagoId)),
    [currentItems]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">Detalle de Pagos del Alumno {alumnoId}</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          type="button"
          onClick={() => navigate("/pagos/formulario")}
          className="page-button-secondary"
        >
          Volver a Pagos
        </Boton>
      </div>
      <div className="page-card">
        {loading && detalles.length === 0 ? (
          <div className="text-center py-4">Cargando...</div>
        ) : error && detalles.length === 0 ? (
          <div className="text-center py-4 text-destructive">{error}</div>
        ) : (
          <Tabla
            headers={[
              "N° Recibo",
              "Alumno",
              "Concepto",
              "Cobrado",
              "Fecha pago",
            ]}
            data={sortedDetalles}
            customRender={(fila: DetallePagoResponse) => [
              fila.pagoId,
              fila.alumno.nombre + " " + fila.alumno.apellido,
              fila.descripcionConcepto,
              fila.ACobrar,
              formatDateArgentino(fila.fechaRegistro),
              <button
                type="button"
                onClick={() => pagosApi.verRecibo(fila.pagoId)}
                className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-sm"
              >
                Ver Factura
              </button>,
              <button
                type="button"
                onClick={() => pagosApi.descargarRecibo(fila.pagoId)}
                className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-sm"
              >
                Descargar Factura
              </button>,
            ]}
          />
        )}
      </div>
      <InfiniteScroll
        onLoadMore={loadMore}
        hasMore={hasMore}
        loading={loading}
        className="mt-4"
        children={undefined}
      >
        {/* Puedes dejar este espacio sin contenido o agregar un mensaje opcional */}
      </InfiniteScroll>
    </div>
  );
};

export default DetallePagoListByAlumno;
