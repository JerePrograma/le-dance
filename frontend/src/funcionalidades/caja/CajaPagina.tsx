"use client";

import React, { useState, useCallback, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosConfig";
import type { CajaResponse, PageResponse } from "../../types/types";
import { toast } from "react-toastify";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";

const CajaPagina: React.FC = () => {
  const [cajas, setCajas] = useState<CajaResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 25;
  const navigate = useNavigate();

  const fetchCajas = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // Se asume que el endpoint admite query params "page" y "size"
      const response = await api.get<PageResponse<CajaResponse>>("/cajas", {
        params: { page, size: itemsPerPage },
      });
      const data = response.data;
      setCajas((prev) => [...prev, ...data.content]);
      setTotalPages(data.totalPages);
      setPage((prev) => prev + 1);
    } catch (err) {
      toast.error("Error al cargar los registros de caja:");
      setError("Error al cargar los registros de caja.");
    } finally {
      setLoading(false);
    }
  }, [page, itemsPerPage]);

  useEffect(() => {
    fetchCajas();
  }, [fetchCajas]);

  // Si totalPages aún no se ha definido, asumimos que hay más datos.
  const hasMore = totalPages === null || page < totalPages;

  if (loading && cajas.length === 0)
    return (
      <div className="flex justify-center items-center p-4 text-gray-600">
        Cargando registros...
      </div>
    );

  if (error && cajas.length === 0)
    return (
      <div className="flex justify-center items-center p-4 text-red-600">
        {error}
      </div>
    );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">
        Caja - Ingresos Diarios
      </h1>

      <div className="flex justify-end mb-6">
        <button
          onClick={() => navigate("/caja/formulario")}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors duration-200 flex items-center gap-2"
          aria-label="Registrar nuevo ingreso"
        >
          Registrar Nuevo Ingreso
        </button>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {[
                  "ID",
                  "Fecha",
                  "Total Efectivo",
                  "Total Transferencia",
                  "Rango",
                  "Observaciones",
                  "Acciones",
                ].map((header) => (
                  <th
                    key={header}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {cajas.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-6 py-4 text-center text-sm text-gray-500 bg-[#FFF5EE]"
                  >
                    No hay transacciones registradas
                  </td>
                </tr>
              ) : (
                cajas.map((fila) => (
                  <tr key={fila.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.fecha}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.totalEfectivo}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.totalDebito}
                    </td>

                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.rangoDesdeHasta}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {fila.observaciones}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      <div className="flex gap-2">
                        <button
                          onClick={() =>
                            navigate(`/caja/formulario?id=${fila.id}`)
                          }
                          className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1 rounded-md text-sm transition-colors duration-200"
                          aria-label={`Editar ingreso ${fila.id}`}
                        >
                          Editar
                        </button>
                        <button
                          className="bg-red-100 hover:bg-red-200 text-red-700 px-3 py-1 rounded-md text-sm transition-colors duration-200"
                          aria-label={`Eliminar ingreso ${fila.id}`}
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <InfiniteScroll
        onLoadMore={fetchCajas}
        hasMore={hasMore}
        loading={loading}
        className="mt-4"
        children={undefined}
      >
        {/* Puedes mostrar aquí un mensaje opcional mientras se cargan más registros */}
      </InfiniteScroll>
    </div>
  );
};

export default CajaPagina;
