"use client";

import React, {
  useState,
  useCallback,
  useEffect,
  useMemo,
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
const estimatedRowHeight = 70; // Altura estimada en píxeles de cada fila
const itemsPerPage = 25; // Valor base (fallback)

const DetallePagoList: React.FC = () => {
  // Estados para datos y carga
  const [detalles, setDetalles] = useState<DetallePagoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [visibleCount, setVisibleCount] = useState<number>(itemsPerPage);

  // Estados para filtros generales
  const [fechaInicio, setFechaInicio] = useState("");
  const [fechaFin, setFechaFin] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");

  // Estados para filtros secundarios
  const [disciplinas, setDisciplinas] = useState<any[]>([]);
  const [selectedDisciplina, setSelectedDisciplina] = useState("");
  const [selectedTarifa, setSelectedTarifa] = useState("");

  const [stocks, setStocks] = useState<any[]>([]);
  const [selectedStock, setSelectedStock] = useState("");

  const [subConceptos, setSubConceptos] = useState<any[]>([]);
  const [selectedSubConcepto, setSelectedSubConcepto] = useState("");
  const [conceptos, setConceptos] = useState<any[]>([]);
  const [selectedConcepto, setSelectedConcepto] = useState("");

  // Refs para calcular alturas
  const containerRef = useRef<HTMLDivElement>(null);
  const tableContainerRef = useRef<HTMLDivElement>(null);

  // Función para cargar los detalles (aplicando filtros si es necesario)
  const fetchDetalles = useCallback(
    async (params: Record<string, string> = {}) => {
      try {
        setLoading(true);
        setError(null);
        const data = await pagosApi.filtrarDetalles(params);
        setDetalles(Array.isArray(data) ? data : []);
        setVisibleCount(itemsPerPage);
      } catch (error) {
        toast.error("Error al cargar pagos pendientes");
        setError("Error al cargar pagos pendientes");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  // Cargar datos al montar
  useEffect(() => {
    fetchDetalles();
  }, [fetchDetalles]);

  // Cargar filtros secundarios según la categoría seleccionada
  useEffect(() => {
    if (filtroTipo === "DISCIPLINAS") {
      disciplinasApi
        .listarDisciplinas()
        .then((data) => setDisciplinas(data))
        .catch(() => toast.error("Error al cargar disciplinas"));
    } else if (filtroTipo === "STOCK") {
      stocksApi
        .listarStocks()
        .then((data) => setStocks(data))
        .catch(() => toast.error("Error al cargar stocks"));
    } else if (filtroTipo === "CONCEPTOS") {
      subConceptosApi
        .listarSubConceptos()
        .then((data) => setSubConceptos(data))
        .catch(() => toast.error("Error al cargar sub conceptos"));
    }
    // Reiniciamos filtros secundarios
    setSelectedDisciplina("");
    setSelectedTarifa("");
    setSelectedStock("");
    setSelectedSubConcepto("");
    setSelectedConcepto("");
  }, [filtroTipo]);

  // Cargar conceptos al seleccionar un sub concepto (para CONCEPTOS)
  useEffect(() => {
    if (filtroTipo === "CONCEPTOS" && selectedSubConcepto) {
      conceptosApi
        .listarConceptosPorSubConcepto(selectedSubConcepto)
        .then((data) => setConceptos(data))
        .catch(() => toast.error("Error al cargar conceptos"));
    } else {
      setConceptos([]);
      setSelectedConcepto("");
    }
  }, [filtroTipo, selectedSubConcepto]);

  // Filtrar detalles para mostrar solo los NO cobrados
  const detallesNoCobrado = useMemo(() => {
    return Array.isArray(detalles)
      ? detalles.filter(
          (detalle) =>
            detalle.importePendiente > 0 &&
            detalle.estadoPago !== "ANULADO" &&
            !detalle.cobrado
        )
      : [];
  }, [detalles]);

  // Ajustar visibleCount según la altura del contenedor principal
  const adjustVisibleCount = useCallback(() => {
    if (containerRef.current) {
      const containerHeight =
        containerRef.current.getBoundingClientRect().height;
      const itemsThatFit = Math.ceil(containerHeight / estimatedRowHeight);
      setVisibleCount(itemsThatFit);
    }
  }, []);

  useEffect(() => {
    adjustVisibleCount();
    window.addEventListener("resize", adjustVisibleCount);
    return () => window.removeEventListener("resize", adjustVisibleCount);
  }, [adjustVisibleCount]);

  // Ajustar la altura del contenedor de la tabla para ocupar el espacio disponible
  useEffect(() => {
    const adjustTableHeight = () => {
      if (tableContainerRef.current) {
        const windowHeight = window.innerHeight;
        const tableRect = tableContainerRef.current.getBoundingClientRect();
        const availableHeight = windowHeight - tableRect.top - 10;
        tableContainerRef.current.style.height = `${availableHeight}px`;
      }
    };

    adjustTableHeight();
    window.addEventListener("resize", adjustTableHeight);
    return () => window.removeEventListener("resize", adjustTableHeight);
  }, []);

  // Subconjunto de datos a mostrar según visibleCount
  const currentItems = useMemo(
    () => detallesNoCobrado.slice(0, visibleCount),
    [detallesNoCobrado, visibleCount]
  );

  // Cálculo de la suma total de "importePendiente" de los elementos visibles
  const totalImportePendiente = useMemo(() => {
    return currentItems.reduce(
      (acc, item) => acc + Number(item.importePendiente || 0),
      0
    );
  }, [currentItems]);

  // Determina si hay más elementos para cargar
  const hasMore = useMemo(
    () => visibleCount < detallesNoCobrado.length,
    [visibleCount, detallesNoCobrado.length]
  );

  // Función para cargar más elementos
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + itemsPerPage);
    }
  }, [hasMore]);

  // Manejo del envío del filtro
  const handleFilterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const params: Record<string, string> = {};
    if (fechaInicio) params.fechaRegistroDesde = fechaInicio;
    if (fechaFin) params.fechaRegistroHasta = fechaFin;
    if (filtroTipo) {
      params.categoria = filtroTipo;
      switch (filtroTipo) {
        case "DISCIPLINAS":
          if (selectedDisciplina) params.disciplina = selectedDisciplina;
          if (selectedTarifa) params.tarifa = selectedTarifa;
          break;
        case "STOCK":
          if (selectedStock) params.stock = selectedStock;
          break;
        case "CONCEPTOS":
          if (selectedSubConcepto) params.subConcepto = selectedSubConcepto;
          if (selectedConcepto) params.detalleConcepto = selectedConcepto;
          break;
        default:
          break;
      }
    }
    console.log("Filtros aplicados:", params);
    await fetchDetalles(params);
    adjustVisibleCount();
  };

  if (loading && detalles.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  const sortedItems = [...currentItems].sort(
    (a, b) => Number(b.pagoId) - Number(a.pagoId)
  );

  return (
    <div ref={containerRef} className="flex flex-col h-screen overflow-hidden">
      {/* Header */}
      <div className="flex-none p-6 pb-2">
        <h1 className="text-3xl font-bold tracking-tight">Pagos pendientes</h1>
      </div>

      {/* Sección de filtros */}
      <div className="flex-none px-6 pb-4">
        <div className="page-card">
          <form onSubmit={handleFilterSubmit}>
            <div className="flex gap-4 items-center mb-4">
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
                  <option value="DISCIPLINAS">DISCIPLINAS</option>
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
                      {tarifaOptions.map((tarifa) => (
                        <option key={tarifa} value={tarifa}>
                          {tarifa}
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
      </div>

      {/* Tabla de Detalles de Pago envuelta en ListaConInfiniteScroll */}
      <div
        ref={tableContainerRef}
        className="flex-grow px-6 pb-0 overflow-hidden"
      >
        <div className="page-card h-full">
          <ListaConInfiniteScroll
            onLoadMore={onLoadMore}
            hasMore={hasMore}
            loading={loading}
            fillAvailable={true}
          >
            <Tabla
              headers={["Código", "Alumno", "Concepto", "Deuda"]}
              data={sortedItems}
              customRender={(fila) => {
                return [
                  fila.pagoId || fila.id,
                  fila.alumno.nombre + " " + fila.alumno.apellido,
                  fila.descripcionConcepto,
                  fila.importePendiente,
                ];
              }}
              emptyMessage="No hay pagos pendientes"
            />
          </ListaConInfiniteScroll>
        </div>
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
