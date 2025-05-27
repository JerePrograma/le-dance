"use client";

import React, { useState, useCallback, useEffect, useMemo } from "react";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import disciplinasApi from "../../api/disciplinasApi";
import stocksApi from "../../api/stocksApi";
import subConceptosApi from "../../api/subConceptosApi";
import conceptosApi from "../../api/conceptosApi";
import type { DetallePagoResponse } from "../../types/types";

const tarifaOptions = ["CUOTA", "CLASE DE PRUEBA", "CLASE SUELTA"];

const DetallePagoList: React.FC = () => {
  const [detalles, setDetalles] = useState<DetallePagoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [fechaInicio, setFechaInicio] = useState("");
  const [fechaFin, setFechaFin] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");

  const [disciplinas, setDisciplinas] = useState<any[]>([]);
  const [selectedDisciplina, setSelectedDisciplina] = useState("");
  const [selectedTarifa, setSelectedTarifa] = useState("");

  const [stocks, setStocks] = useState<any[]>([]);
  const [selectedStock, setSelectedStock] = useState("");

  const [subConceptos, setSubConceptos] = useState<any[]>([]);
  const [selectedSubConcepto, setSelectedSubConcepto] = useState("");
  const [conceptos, setConceptos] = useState<any[]>([]);
  const [selectedConcepto, setSelectedConcepto] = useState("");

  const fetchDetalles = useCallback(
    async (params: Record<string, string> = {}) => {
      try {
        setLoading(true);
        setError(null);
        const data = await pagosApi.filtrarDetalles(params);
        setDetalles(Array.isArray(data) ? data : []);
      } catch {
        toast.error("Error al cargar pagos pendientes");
        setError("Error al cargar pagos pendientes");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    fetchDetalles();
  }, [fetchDetalles]);

  useEffect(() => {
    if (filtroTipo === "DISCIPLINAS") {
      disciplinasApi
        .listarDisciplinas()
        .then(setDisciplinas)
        .catch(() => toast.error("Error al cargar disciplinas"));
    } else if (filtroTipo === "STOCK") {
      stocksApi
        .listarStocks()
        .then(setStocks)
        .catch(() => toast.error("Error al cargar stocks"));
    } else if (filtroTipo === "CONCEPTOS") {
      subConceptosApi
        .listarSubConceptos()
        .then(setSubConceptos)
        .catch(() => toast.error("Error al cargar sub conceptos"));
    }
    setSelectedDisciplina("");
    setSelectedTarifa("");
    setSelectedStock("");
    setSelectedSubConcepto("");
    setSelectedConcepto("");
  }, [filtroTipo]);

  useEffect(() => {
    if (filtroTipo === "CONCEPTOS" && selectedSubConcepto) {
      conceptosApi
        .listarConceptosPorSubConcepto(selectedSubConcepto)
        .then(setConceptos)
        .catch(() => toast.error("Error al cargar conceptos"));
    } else {
      setConceptos([]);
      setSelectedConcepto("");
    }
  }, [filtroTipo, selectedSubConcepto]);

  const detallesNoCobrado = useMemo(
    () =>
      detalles.filter(
        (d) => d.importePendiente > 0 && d.estadoPago !== "ANULADO"
      ),
    [detalles]
  );

  const totalImportePendiente = useMemo(
    () =>
      detallesNoCobrado.reduce(
        (acc, item) => acc + Number(item.importePendiente || 0),
        0
      ),
    [detallesNoCobrado]
  );

  const handleFilterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const params: Record<string, string> = {};
    if (fechaInicio) params.fechaRegistroDesde = fechaInicio;
    if (fechaFin) params.fechaRegistroHasta = fechaFin;
    if (filtroTipo) {
      params.categoria = filtroTipo;
      if (filtroTipo === "DISCIPLINAS") {
        if (selectedDisciplina) params.disciplina = selectedDisciplina;
        if (selectedTarifa) params.tarifa = selectedTarifa;
      } else if (filtroTipo === "STOCK") {
        if (selectedStock) params.stock = selectedStock;
      } else if (filtroTipo === "CONCEPTOS") {
        if (selectedSubConcepto) params.subConcepto = selectedSubConcepto;
        if (selectedConcepto) params.detalleConcepto = selectedConcepto;
      }
    }
    await fetchDetalles(params);
  };

  if (loading && detalles.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  const sortedItems = [...detallesNoCobrado].sort(
    (a, b) => Number(b.pagoId) - Number(a.pagoId)
  );

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-4">Pagos pendientes</h1>

      <div className="page-card mb-6 p-4">
        <form onSubmit={handleFilterSubmit}>
          <div className="flex gap-4 items-center mb-4">
            {/* Filtros de fecha y tipo */}
            <div>
              <label className="block font-medium">Desde</label>
              <input
                type="date"
                className="border p-2"
                value={fechaInicio}
                onChange={(e) => setFechaInicio(e.target.value)}
              />
            </div>
            <div>
              <label className="block font-medium">Hasta</label>
              <input
                type="date"
                className="border p-2"
                value={fechaFin}
                onChange={(e) => setFechaFin(e.target.value)}
              />
            </div>
            <div>
              <label className="block font-medium">Filtrar por:</label>
              <select
                className="border p-2"
                value={filtroTipo}
                onChange={(e) => setFiltroTipo(e.target.value)}
              >
                <option value="">Seleccionar...</option>
                <option value="DISCIPLINIAS">DISCIPLINAS</option>
                <option value="STOCK">STOCK</option>
                <option value="CONCEPTOS">CONCEPTOS</option>
                <option value="MATRICULA">MATRICULA</option>
              </select>
            </div>

            {filtroTipo === "DISCIPLINIAS" && (
              <>
                <div>
                  <label className="block font-medium">Disciplina</label>
                  <select
                    className="border p-2"
                    value={selectedDisciplina}
                    onChange={(e) => setSelectedDisciplina(e.target.value)}
                  >
                    <option value="">Seleccionar disciplina...</option>
                    {disciplinas.map((d) => (
                      <option key={d.id} value={d.nombre || d.descripcion}>
                        {d.nombre || d.descripcion}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-medium">Tarifa</label>
                  <select
                    className="border p-2"
                    value={selectedTarifa}
                    onChange={(e) => setSelectedTarifa(e.target.value)}
                  >
                    <option value="">Seleccionar tarifa...</option>
                    {tarifaOptions.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            {filtroTipo === "STOCK" && (
              <div>
                <label className="block font-medium">Stock</label>
                <select
                  className="border p-2"
                  value={selectedStock}
                  onChange={(e) => setSelectedStock(e.target.value)}
                >
                  <option value="">Seleccionar stock...</option>
                  {stocks.map((s) => (
                    <option key={s.id} value={s.nombre || s.descripcion}>
                      {s.nombre || s.descripcion}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {filtroTipo === "CONCEPTOS" && (
              <>
                <div>
                  <label className="block font-medium">Sub Concepto</label>
                  <select
                    className="border p-2"
                    value={selectedSubConcepto}
                    onChange={(e) => setSelectedSubConcepto(e.target.value)}
                  >
                    <option value="">Seleccionar sub concepto...</option>
                    {subConceptos.map((sc) => (
                      <option key={sc.id} value={sc.descripcion}>
                        {sc.descripcion}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-medium">Concepto</label>
                  <select
                    className="border p-2"
                    value={selectedConcepto}
                    onChange={(e) => setSelectedConcepto(e.target.value)}
                    disabled={!selectedSubConcepto || conceptos.length === 0}
                  >
                    <option value="">Seleccionar concepto...</option>
                    {conceptos.map((c) => (
                      <option key={c.id} value={c.descripcion}>
                        {c.descripcion}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            <Boton
              type="submit"
              className="bg-green-500 text-white p-2 rounded"
            >
              Ver
            </Boton>
          </div>
        </form>
      </div>

      <div className="page-card mb-4 p-4 overflow-auto">
        <Tabla
          headers={["Código", "Alumno", "Concepto", "Deuda"]}
          data={sortedItems}
          customRender={(fila) => [
            fila.pagoId || fila.id,
            `${fila.alumno.nombre} ${fila.alumno.apellido}`,
            fila.descripcionConcepto,
            fila.importePendiente,
          ]}
          emptyMessage="No hay pagos pendientes"
        />
      </div>

     {/* Footer con la suma total de "importePendiente" de los elementos visibles */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div className="bg-gray-50 rounded-lg shadow p-4">
          <p className="text-right text-xl font-bold text-gray-900">
            Deuda total: {totalImportePendiente}
          </p>
        </div>
      </div>
    </div>
  );
};

export default DetallePagoList;
