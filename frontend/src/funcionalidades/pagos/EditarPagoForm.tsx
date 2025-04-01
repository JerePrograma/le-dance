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
import detallesPagoApi from "../../api/detallesPagoApi";
import disciplinasApi from "../../api/disciplinasApi";
import stocksApi from "../../api/stocksApi";
import subConceptosApi from "../../api/subConceptosApi";
import conceptosApi from "../../api/conceptosApi";
import alumnosApi from "../../api/alumnosApi";
import type { DetallePagoResponse, AlumnoResponse } from "../../types/types";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";
import useDebounce from "../../hooks/useDebounce";
import { X } from "lucide-react";

interface DetallePagoListProps {
  // Si se pasa por prop se filtrará de forma fija; de lo contrario se podrá buscar y seleccionar
  alumnoId?: string;
}

const tarifaOptions = ["CUOTA", "CLASE DE PRUEBA", "CLASE SUELTA"];
const estimatedRowHeight = 70; // Altura estimada por fila (en píxeles)
const itemsPerPage = 25; // Valor base (fallback)

const DetallePagoList: React.FC<DetallePagoListProps> = ({
  alumnoId: alumnoIdProp,
}) => {
  // Estados de datos y carga
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

  // Estados para búsqueda y autocompletar alumnos
  const [nombreBusqueda, setNombreBusqueda] = useState("");
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<
    AlumnoResponse[]
  >([]);
  const [activeSuggestionIndex, setActiveSuggestionIndex] =
    useState<number>(-1);
  const [showSuggestions, setShowSuggestions] = useState(false);
  // Si no se pasó alumnoId por prop, se usa el que seleccione el usuario
  const [alumnoIdLocal, setAlumnoIdLocal] = useState<string | null>(null);
  const searchWrapperRef = useRef<HTMLDivElement>(null);
  const debouncedNombreBusqueda = useDebounce(nombreBusqueda, 300);

  // Ref para calcular alturas
  const containerRef = useRef<HTMLDivElement>(null);
  const tableContainerRef = useRef<HTMLDivElement>(null);

  // Hook para bonificaciones y recargos
  const { bonificaciones, recargos } = useCobranzasData();

  // Función para cargar detalles (aplicando filtros si existen)
  const fetchDetalles = useCallback(
    async (params: Record<string, string> = {}) => {
      try {
        setLoading(true);
        setError(null);
        const data = await pagosApi.filtrarDetalles(params);
        setDetalles(Array.isArray(data) ? data : []);
        // Reiniciamos visibleCount al recargar datos
        setVisibleCount(itemsPerPage);
      } catch (error) {
      } finally {
        setLoading(false);
      }
    },
    []
  );

  // Determinar el alumno a filtrar: prioriza la prop; de lo contrario, el seleccionado localmente
  const alumnoFiltro = alumnoIdProp || (alumnoIdLocal ? alumnoIdLocal : "");

  // Al montar el componente, incluimos el filtro por alumno si existe
  useEffect(() => {
    const params: Record<string, string> = {};
    if (alumnoFiltro) {
      params.alumnoId = alumnoFiltro;
    }
    fetchDetalles(params);
  }, [fetchDetalles, alumnoFiltro]);

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

  // Buscar sugerencias de alumnos cuando cambia el nombre (con debounce)
  useEffect(() => {
    const buscarSugerencias = async () => {
      const query = debouncedNombreBusqueda.trim();
      if (query !== "") {
        try {
          const sugerencias = await alumnosApi.buscarPorNombre(query);
          setSugerenciasAlumnos(sugerencias);
        } catch (error) {
          setSugerenciasAlumnos([]);
        }
      } else {
        setSugerenciasAlumnos([]);
      }
      setActiveSuggestionIndex(-1);
    };
    buscarSugerencias();
  }, [debouncedNombreBusqueda]);

  // Manejo de teclas para navegación en las sugerencias
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (sugerenciasAlumnos.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev < sugerenciasAlumnos.length - 1 ? prev + 1 : 0
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev > 0 ? prev - 1 : sugerenciasAlumnos.length - 1
        );
      } else if (e.key === "Enter" || e.key === "Tab") {
        if (
          activeSuggestionIndex >= 0 &&
          activeSuggestionIndex < sugerenciasAlumnos.length
        ) {
          e.preventDefault();
          const alumnoSeleccionado = sugerenciasAlumnos[activeSuggestionIndex];
          handleSeleccionarAlumno(alumnoSeleccionado);
        }
      }
    }
  };

  // Manejar selección de alumno desde las sugerencias
  const handleSeleccionarAlumno = (alumno: AlumnoResponse) => {
    setAlumnoIdLocal(String(alumno.id));
    setNombreBusqueda(`${alumno.nombre} ${alumno.apellido}`);
    setSugerenciasAlumnos([]);
    setShowSuggestions(false);
  };

  // Ref para cerrar sugerencias al hacer clic fuera
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        searchWrapperRef.current &&
        !searchWrapperRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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

  // Ajustar la altura del contenedor de la tabla para ocupar todo el espacio disponible
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

  // Filtrar los detalles que cumplen: aCobrar > 0 || importePendiente === 0 || estadoPago === "ANULADO"
  const filteredDetalles = useMemo(() => {
    return detalles.filter(
      (d) =>
        d.aCobrar > 0 ||
        d.importePendiente === 0 ||
        d.estadoPago.toUpperCase() === "ANULADO" // Se compara en mayúsculas para mayor seguridad
    );
  }, [detalles]);

  // Tomamos la cantidad visible del array filtrado
  const currentItems = useMemo(
    () => filteredDetalles.slice(0, visibleCount),
    [filteredDetalles, visibleCount]
  );

  // Ordenamos los ítems (por ejemplo, de mayor a menor ID)
  const sortedItems = useMemo(
    () => [...currentItems].sort((a, b) => Number(b.pagoId) - Number(a.pagoId)),
    [currentItems]
  );

  // Actualizamos también el indicador de "más elementos" para el infinite scroll
  const hasMore = useMemo(
    () => visibleCount < filteredDetalles.length,
    [visibleCount, filteredDetalles.length]
  );

  // Función para cargar más datos (incrementa visibleCount)
  const onLoadMore = useCallback(() => {
    if (hasMore) {
      setVisibleCount((prev) => prev + itemsPerPage);
    }
  }, [hasMore]);

  // Función para eliminar un detalle de pago (usando el objeto detalle)
  const handleDeleteDetalle = async (detalle: DetallePagoResponse) => {
    if (window.confirm("¿Está seguro de eliminar este detalle?")) {
      if (detalle.id && Number(detalle.id) !== 0) {
        try {
          await detallesPagoApi.eliminarDetallePago(detalle.id);
          toast.success("Detalle eliminado correctamente");
        } catch (error) {
          toast.error("Error al eliminar el detalle");
          return;
        }
      }
      setDetalles((prev) => prev.filter((d) => d.id !== detalle.id));
    }
  };

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleAnularDetalle = async (detalle: DetallePagoResponse) => {
    if (!detalle.id || Number(detalle.id) === 0 || isSubmitting) return;
    setIsSubmitting(true);
    try {
      const detalleActualizado = await detallesPagoApi.anularDetallePago(
        detalle.id
      );
      if (!detalleActualizado) {
        // O bien re-fetch de la lista
        await fetchDetalles();
        toast.success("Detalle anulado correctamente");
        return;
      }
      toast.success("Detalle anulado correctamente");
      setDetalles((prevDetalles) =>
        prevDetalles.map((d) => (d.id === detalle.id ? detalleActualizado : d))
      );
    } catch (error) {
      toast.error("Error al anular el detalle");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Manejo del envío del filtro
  const handleFilterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const params: Record<string, string> = {};
    if (fechaInicio) params.fechaRegistroDesde = fechaInicio;
    if (fechaFin) params.fechaRegistroHasta = fechaFin;
    if (alumnoFiltro) {
      params.alumnoId = alumnoFiltro;
    }
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
  if (error && detalles.length === 0)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div ref={containerRef} className="flex flex-col h-screen overflow-hidden">
      {/* Header */}
      <div className="flex-none p-6 pb-2">
        <h1 className="text-3xl font-bold tracking-tight">Pagos cobrados</h1>
      </div>

      {/* Sección de filtros */}
      <div className="flex-none px-6 pb-4">
        <div className="page-card">
          <form onSubmit={handleFilterSubmit}>
            <div className="flex flex-col gap-4 mb-4">
              {/* Campo de búsqueda por Alumno con autocompletar */}
              <div className="relative">
                <label htmlFor="nombreBusqueda" className="block font-medium">
                  Buscar Alumno:
                </label>
                <input
                  type="text"
                  id="nombreBusqueda"
                  value={nombreBusqueda}
                  onChange={(e) => {
                    const value = e.target.value;
                    setNombreBusqueda(value);
                    setShowSuggestions(value.trim() !== "");
                  }}
                  onFocus={() => {
                    if (nombreBusqueda.trim() !== "") {
                      setShowSuggestions(true);
                    }
                  }}
                  onKeyDown={handleKeyDown}
                  className="border p-2 w-full"
                />
                {nombreBusqueda && (
                  <button
                    type="button"
                    onClick={() => {
                      setNombreBusqueda("");
                      setSugerenciasAlumnos([]);
                      setShowSuggestions(false);
                      setAlumnoIdLocal(null);
                    }}
                    className="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
                {showSuggestions && sugerenciasAlumnos.length > 0 && (
                  <ul className="sugerencias-lista absolute w-full bg-[hsl(var(--popover))] border border-[hsl(var(--border))] z-10">
                    {sugerenciasAlumnos.map((alumno, index) => (
                      <li
                        key={alumno.id}
                        onClick={() => handleSeleccionarAlumno(alumno)}
                        onMouseEnter={() => setActiveSuggestionIndex(index)}
                        className={`sugerencia-item p-2 cursor-pointer ${
                          index === activeSuggestionIndex
                            ? "bg-[hsl(var(--muted))]"
                            : ""
                        }`}
                      >
                        {alumno.nombre} {alumno.apellido}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              {/* Resto de filtros: fechas y por categoría */}
              <div className="flex gap-4 items-center">
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
                        disabled={
                          !selectedSubConcepto || conceptos.length === 0
                        }
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
            </div>
          </form>
        </div>
      </div>

      {/* Tabla de Detalles de Pago con infinite scroll */}
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
              headers={[
                "Código",
                "Alumno",
                "Concepto",
                "Cobrado",
                "Bonificación",
                "Recargo",
                "Cobrados",
                "Estado",
              ]}
              data={sortedItems}
              customRender={(fila: DetallePagoResponse) => {
                const bonificacionNombre =
                  fila.bonificacionId &&
                  bonificaciones.find((b) => b.id === fila.bonificacionId)
                    ?.descripcion;
                const recargoNombre =
                  fila.recargoId &&
                  recargos.find((r) => r.id === Number(fila.recargoId))
                    ?.descripcion;
                return [
                  fila.id,
                  fila.alumnoDisplay,
                  fila.descripcionConcepto,
                  fila.aCobrar,
                  bonificacionNombre || "-",
                  recargoNombre || "-",
                  fila.cobrado ? "Sí" : "No",
                  fila.estadoPago,
                ];
              }}
              actions={(fila: DetallePagoResponse) => (
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={isSubmitting}
                    className="bg-green-500 hover:bg-green-600 text-white p-1 rounded text-xs transition-colors mx-auto block"
                    onClick={() => handleAnularDetalle(fila)}
                  >
                    Anular
                  </button>
                  <button
                    type="button"
                    className="bg-red-500 hover:bg-red-600 text-white p-1 rounded text-xs transition-colors mx-auto block"
                    onClick={() => handleDeleteDetalle(fila)}
                  >
                    Eliminar
                  </button>
                </div>
              )}
              emptyMessage="No hay pagos cobrados"
            />
          </ListaConInfiniteScroll>
        </div>
      </div>
    </div>
  );
};

export default DetallePagoList;
