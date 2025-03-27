"use client";

import React, { useState, useCallback, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import disciplinasApi from "../../api/disciplinasApi";
import stocksApi from "../../api/stocksApi";
import subConceptosApi from "../../api/subConceptosApi";
import conceptosApi from "../../api/conceptosApi";
import type { DetallePagoResponse } from "../../types/types";
import InfiniteScroll from "../../componentes/comunes/InfiniteScroll";

const tarifaOptions = ["CUOTA", "CLASE DE PRUEBA", "CLASE SUELTA"];

const DetallePagoList: React.FC = () => {
  const [detalles, setDetalles] = useState<DetallePagoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Eliminamos la paginación tradicional y usamos visibleCount
  const [visibleCount, setVisibleCount] = useState<number>(0);
  const itemsPerLoad = 5;
  const navigate = useNavigate();

  // Estados para filtros generales
  const [fechaInicio, setFechaInicio] = useState("");
  const [fechaFin, setFechaFin] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");

  // Estados para los filtros secundarios según la opción elegida
  const [disciplinas, setDisciplinas] = useState<any[]>([]);
  const [selectedDisciplina, setSelectedDisciplina] = useState("");
  const [selectedTarifa, setSelectedTarifa] = useState("");

  const [stocks, setStocks] = useState<any[]>([]);
  const [selectedStock, setSelectedStock] = useState("");

  const [subConceptos, setSubConceptos] = useState<any[]>([]);
  const [selectedSubConcepto, setSelectedSubConcepto] = useState("");
  const [conceptos, setConceptos] = useState<any[]>([]);
  const [selectedConcepto, setSelectedConcepto] = useState("");

  // Función para traer los detalles, aplicando filtros si existen.
  const fetchDetalles = useCallback(
    async (params: Record<string, string> = {}) => {
      try {
        setLoading(true);
        setError(null);
        const data = await pagosApi.filtrarDetalles(params);
        setDetalles(data);
        // Al aplicar filtros reiniciamos la cantidad de elementos visibles
        setVisibleCount(itemsPerLoad);
      } catch (error) {
        toast.error("Error al cargar pagos pendientes");
        setError("Error al cargar pagos pendientes");
      } finally {
        setLoading(false);
      }
    },
    [itemsPerLoad]
  );

  // Al montar el componente se trae la lista completa sin filtros.
  useEffect(() => {
    fetchDetalles();
  }, [fetchDetalles]);

  // Carga de filtros secundarios según la categoría seleccionada.
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
    // Reiniciamos los filtros secundarios al cambiar la categoría
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

  // Filtrar los detalles para mostrar solo aquellos que han sido cobrados (o ajustar según el requerimiento)
  const detallesNoCobrado = useMemo(
    () => detalles.filter((detalle) => detalle.cobrado),
    [detalles]
  );

  // Los elementos actualmente visibles, basados en visibleCount
  const currentItems = useMemo(
    () => detallesNoCobrado.slice(0, visibleCount),
    [detallesNoCobrado, visibleCount]
  );

  // Determina si quedan más elementos por mostrar
  const hasMore = visibleCount < detallesNoCobrado.length;

  // Función que incrementa la cantidad de elementos visibles
  const loadMore = useCallback(() => {
    setVisibleCount((prev) =>
      Math.min(prev + itemsPerLoad, detallesNoCobrado.length)
    );
  }, [detallesNoCobrado.length, itemsPerLoad]);

  // Al enviar el formulario se arma el objeto de parámetros.
  const handleFilterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const params: Record<string, string> = {};
    if (fechaInicio) params.fechaRegistroDesde = fechaInicio;
    if (fechaFin) params.fechaRegistroHasta = fechaFin;

    if (filtroTipo) {
      params.categoria = filtroTipo;
      switch (filtroTipo) {
        case "DISCIPLINAS":
          if (selectedDisciplina) {
            params.disciplina = selectedDisciplina;
          }
          if (selectedTarifa) {
            params.tarifa = selectedTarifa;
          }
          break;
        case "STOCK":
          if (selectedStock) {
            params.stock = selectedStock;
          }
          break;
        case "CONCEPTOS":
          if (selectedSubConcepto) {
            params.subConcepto = selectedSubConcepto;
          }
          if (selectedConcepto) {
            params.detalleConcepto = selectedConcepto;
          }
          break;
        default:
          break;
      }
    }

    console.log("Filtros aplicados:", params);
    await fetchDetalles(params);
  };

  if (loading && detalles.length === 0)
    return <div className="text-center py-4">Cargando...</div>;
  if (error && detalles.length === 0)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Pagos pendientes</h1>

      {/* Sección de filtros */}
      <div className="page-card mb-4">
        <form onSubmit={handleFilterSubmit}>
          <div className="flex gap-4 items-center mb-4">
            <div>
              <label className="block font-medium">Desde</label>
              <input
                type="date"
                value={fechaInicio}
                onChange={(e) => setFechaInicio(e.target.value)}
                className="border p-2"
              />
            </div>
            <div>
              <label className="block font-medium">Hasta</label>
              <input
                type="date"
                value={fechaFin}
                onChange={(e) => setFechaFin(e.target.value)}
                className="border p-2"
              />
            </div>
            <div>
              <label className="block font-medium">Filtrar por:</label>
              <select
                value={filtroTipo}
                onChange={(e) => setFiltroTipo(e.target.value)}
                className="border p-2"
              >
                <option value="">Seleccionar...</option>
                <option value="DISCIPLINAS">DISCIPLINAS</option>
                <option value="STOCK">STOCK</option>
                <option value="CONCEPTOS">CONCEPTOS</option>
              </select>
            </div>

            {filtroTipo === "DISCIPLINAS" && (
              <>
                <div>
                  <label className="block font-medium">Disciplina</label>
                  <select
                    value={selectedDisciplina}
                    onChange={(e) => setSelectedDisciplina(e.target.value)}
                    className="border p-2"
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
                    value={selectedTarifa}
                    onChange={(e) => setSelectedTarifa(e.target.value)}
                    className="border p-2"
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
                  value={selectedStock}
                  onChange={(e) => setSelectedStock(e.target.value)}
                  className="border p-2"
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
                    value={selectedSubConcepto}
                    onChange={(e) => setSelectedSubConcepto(e.target.value)}
                    className="border p-2"
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
                    value={selectedConcepto}
                    onChange={(e) => setSelectedConcepto(e.target.value)}
                    className="border p-2"
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

      {/* Tabla de Detalles de Pago */}
      <div className="page-card">
        {loading && <div className="text-center py-4">Cargando...</div>}
        {error && (
          <div className="text-center py-4 text-destructive">{error}</div>
        )}
        {!loading && !error && (
          <Tabla
            headers={[
              "Código",
              "Alumno",
              "Concepto",
              "Valor Base",
              "Bonificación",
              "Recargo",
              "Cobrados",
            ]}
            data={currentItems}
            customRender={(fila) => [
              fila.conceptoId || fila.id,
              fila.alumnoDisplay,
              fila.descripcionConcepto,
              fila.importeInicial,
              fila.bonificacionId ? fila.bonificacionId : "-",
              fila.recargoId ? fila.recargoId : "-",
              fila.cobrado ? "Sí" : "No",
            ]}
            actions={(fila) => (
              <div className="flex gap-2">
                <Boton
                  onClick={() =>
                    navigate(`/detalles-pago/formulario?id=${fila.id}`)
                  }
                  className="page-button-secondary"
                  aria-label={`Editar detalle de pago ${fila.id}`}
                >
                  Editar
                </Boton>
              </div>
            )}
          />
        )}
      </div>

      {/* Componente Infinite Scroll para cargar progresivamente más elementos */}
      <InfiniteScroll
        onLoadMore={loadMore}
        hasMore={hasMore}
        loading={loading}
        className="mt-4"
        children={undefined}
      />
    </div>
  );
};

export default DetallePagoList;
