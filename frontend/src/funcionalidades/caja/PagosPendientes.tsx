"use client";

import React, {
  useState,
  useEffect,
  useMemo,
  useCallback,
  useRef,
} from "react";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import disciplinasApi from "../../api/disciplinasApi";
import stocksApi from "../../api/stocksApi";
import subConceptosApi from "../../api/subConceptosApi";
import conceptosApi from "../../api/conceptosApi";
import type { DetallePagoResponse } from "../../types/types";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";

const tarifaOptions = ["CUOTA", "CLASE DE PRUEBA", "CLASE SUELTA"];
const estimatedRowHeight = 70;
const itemsPerPage = 25;

const DetallePagoList: React.FC = () => {
  const [detalles, setDetalles] = useState<DetallePagoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [visibleCount, setVisibleCount] = useState(itemsPerPage);

  // filtros generales
  const [fechaInicio, setFechaInicio] = useState("");
  const [fechaFin, setFechaFin] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");

  // filtros secundarios (IDs como strings)
  const [disciplinas, setDisciplinas] = useState<any[]>([]);
  const [selectedDisciplina, setSelectedDisciplina] = useState("");
  const [selectedTarifa, setSelectedTarifa] = useState("");

  const [stocks, setStocks] = useState<any[]>([]);
  const [selectedStock, setSelectedStock] = useState("");

  const [subConceptos, setSubConceptos] = useState<any[]>([]);
  const [selectedSubConcepto, setSelectedSubConcepto] = useState("");
  const [conceptos, setConceptos] = useState<any[]>([]);
  const [selectedConcepto, setSelectedConcepto] = useState("");

  const containerRef = useRef<HTMLDivElement>(null);
  const tableContainerRef = useRef<HTMLDivElement>(null);

  const fetchAll = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await pagosApi.listarDetallesPorFecha({});
      setDetalles(Array.isArray(data) ? data : []);
    } catch {
      toast.error("Error al cargar detalles de pago");
      setError("Error al cargar detalles de pago");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  useEffect(() => {
    // reset secundarios
    setSelectedDisciplina("");
    setSelectedTarifa("");
    setSelectedStock("");
    setSelectedSubConcepto("");
    setSelectedConcepto("");

    if (filtroTipo === "DISCIPLINAS") {
      disciplinasApi
        .listarDisciplinas()
        .then(setDisciplinas)
        .catch(() => toast.error("Error al cargar disciplinas"));
    }
    if (filtroTipo === "STOCK") {
      stocksApi
        .listarStocks()
        .then(setStocks)
        .catch(() => toast.error("Error al cargar stocks"));
    }
    if (filtroTipo === "CONCEPTOS") {
      subConceptosApi
        .listarSubConceptos()
        .then(setSubConceptos)
        .catch(() => toast.error("Error al cargar sub conceptos"));
    }
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

  const detallesFiltrados = useMemo(() => {
    const fi = fechaInicio ? new Date(fechaInicio) : null;
    const ff = fechaFin ? new Date(fechaFin) : null;

    return detalles.filter((det) => {
      if (
        det.importePendiente <= 0 ||
        det.estadoPago === "ANULADO" ||
        det.cobrado
      )
        return false;
      const dr = new Date(det.fechaRegistro);
      if (fi && dr < fi) return false;
      if (ff && dr > ff) return false;

      if (filtroTipo) {
        const tipo = det.tipo?.toUpperCase() || "";
        const desc = det.descripcionConcepto?.toUpperCase() || "";

        switch (filtroTipo) {
          case "DISCIPLINAS":
            if (tipo !== "MENSUALIDAD") return false;
            if (
              selectedDisciplina &&
              !desc.startsWith(selectedDisciplina.toUpperCase())
            )
              return false;
            if (selectedTarifa && !desc.includes(selectedTarifa.toUpperCase()))
              return false;
            break;

          case "STOCK":
            if (tipo !== "STOCK") return false;
            if (selectedStock && String(det.stockId) !== selectedStock)
              return false;
            break;

          case "CONCEPTOS":
            if (tipo !== "CONCEPTO") return false;
            if (
              selectedSubConcepto &&
              String(det.subConceptoId) !== selectedSubConcepto
            )
              return false;
            if (selectedConcepto && String(det.conceptoId) !== selectedConcepto)
              return false;
            break;

          case "MATRICULA":
            if (!desc.includes("MATRICULA")) return false;
            break;

          default:
        }
      }

      return true;
    });
  }, [
    detalles,
    fechaInicio,
    fechaFin,
    filtroTipo,
    selectedDisciplina,
    selectedTarifa,
    selectedStock,
    selectedSubConcepto,
    selectedConcepto,
  ]);

  const currentItems = useMemo(
    () => detallesFiltrados.slice(0, visibleCount),
    [detallesFiltrados, visibleCount]
  );
  const hasMore = visibleCount < detallesFiltrados.length;
  const onLoadMore = useCallback(() => {
    if (hasMore) setVisibleCount((v) => v + itemsPerPage);
  }, [hasMore]);

  const totalImportePendiente = useMemo(
    () =>
      currentItems.reduce((sum, d) => sum + Number(d.importePendiente || 0), 0),
    [currentItems]
  );

  const adjustVisibleCount = useCallback(() => {
    if (containerRef.current) {
      const h = containerRef.current.getBoundingClientRect().height;
      setVisibleCount(Math.ceil(h / estimatedRowHeight));
    }
  }, []);
  useEffect(() => {
    adjustVisibleCount();
    window.addEventListener("resize", adjustVisibleCount);
    return () => window.removeEventListener("resize", adjustVisibleCount);
  }, [adjustVisibleCount]);

  useEffect(() => {
    const adjust = () => {
      if (tableContainerRef.current) {
        const top = tableContainerRef.current.getBoundingClientRect().top;
        tableContainerRef.current.style.height = `${
          window.innerHeight - top - 10
        }px`;
      }
    };
    adjust();
    window.addEventListener("resize", adjust);
    return () => window.removeEventListener("resize", adjust);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setVisibleCount(itemsPerPage);
  };

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  const sortedItems = [...currentItems].sort(
    (a, b) => Number(b.pagoId || b.id) - Number(a.pagoId || a.id)
  );

  return (
    <div ref={containerRef} className="flex flex-col h-screen overflow-hidden">
      <div className="flex-none p-6 pb-2">
        <h1 className="text-3xl font-bold">Pagos pendientes</h1>
      </div>

      <div className="flex-none px-6 pb-4 page-card">
        <form
          onSubmit={handleSubmit}
          className="flex gap-4 items-end flex-wrap"
        >
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
            <label className="block font-medium">Filtrar por</label>
            <select
              className="border p-2"
              value={filtroTipo}
              onChange={(e) => setFiltroTipo(e.target.value)}
            >
              <option value="">Seleccionar...</option>
              <option value="DISCIPLINAS">MENSUALIDADES</option>
              <option value="STOCK">STOCK</option>
              <option value="CONCEPTOS">CONCEPTOS</option>
              <option value="MATRICULA">MATRICULA</option>
            </select>
          </div>

          {filtroTipo === "DISCIPLINAS" && (
            <>
              <div>
                <label className="block font-medium">Disciplina</label>
                <select
                  className="border p-2"
                  value={selectedDisciplina}
                  onChange={(e) => setSelectedDisciplina(e.target.value)}
                >
                  <option value="">Todas</option>
                  {disciplinas.map((d) => (
                    <option key={d.id} value={String(d.id)}>
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
                  <option value="">Todas</option>
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
                <option value="">Todos</option>
                {stocks.map((s) => (
                  <option key={s.id} value={String(s.id)}>
                    {s.nombre || s.descripcion}
                  </option>
                ))}
              </select>
            </div>
          )}

          {filtroTipo === "CONCEPTOS" && (
            <>
              <div>
                <label className="block font-medium">SubConcepto</label>
                <select
                  className="border p-2"
                  value={selectedSubConcepto}
                  onChange={(e) => setSelectedSubConcepto(e.target.value)}
                >
                  <option value="">Todos</option>
                  {subConceptos.map((sc) => (
                    <option key={sc.id} value={String(sc.id)}>
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
                  disabled={!selectedSubConcepto}
                >
                  <option value="">Todos</option>
                  {conceptos.map((c) => (
                    <option key={c.id} value={String(c.id)}>
                      {c.descripcion}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}

          <Boton type="submit" className="bg-green-500 text-white p-2 rounded">
            Aplicar
          </Boton>
        </form>
      </div>

      <div ref={tableContainerRef} className="flex-grow px-6 overflow-hidden">
        <ListaConInfiniteScroll
          onLoadMore={onLoadMore}
          hasMore={hasMore}
          loading={loading}
          fillAvailable
        >
          <Tabla
            headers={["Código", "Alumno", "Concepto", "Deuda"]}
            data={sortedItems}
            customRender={(fila) => [
              fila.pagoId || fila.id,
              fila.alumno.nombre + " " + fila.alumno.apellido,
              fila.descripcionConcepto,
              fila.importePendiente,
            ]}
            emptyMessage="No hay pagos pendientes"
          />
        </ListaConInfiniteScroll>
      </div>

      <div className="px-6 py-4 bg-gray-50">
        <p className="text-right font-bold text-xl">
          Deuda total: {totalImportePendiente}
        </p>
      </div>
    </div>
  );
};

export default DetallePagoList;
