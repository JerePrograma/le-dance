"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { PlusCircle } from "lucide-react";
import { toast } from "react-toastify";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";
import egresosApi from "../../api/egresosApi";
import pagosApi from "../../api/pagosApi";
import type {
  EgresoResponse,
  EgresoRegistroRequest,
  DetallePagoResponse,
  PagoResponse,
} from "../../types/types";

export default function EgresosDebitoPagina() {
  const itemsPerPage = 25;

  // Estados para egresos, infinite scroll y carga
  const [egresos, setEgresos] = useState<EgresoResponse[]>([]);
  const [visibleCount, setVisibleCount] = useState<number>(itemsPerPage);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Estados para el modal de agregar egreso
  const [showModal, setShowModal] = useState<boolean>(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");
  const [fecha, setFecha] = useState<string>(
    new Date().toISOString().split("T")[0]
  );

  // Estados para filtros de fecha (se aplican a egresos y a DetallePago)
  const [filterStartDate, setFilterStartDate] = useState<string>("");
  const [filterEndDate, setFilterEndDate] = useState<string>("");

  // Estados para DetallePago con método DEBITO
  const [detallesDebito, setDetallesDebito] = useState<DetallePagoResponse[]>(
    []
  );
  const [visibleCountPagos, setVisibleCountPagos] =
    useState<number>(itemsPerPage);
  const [loadingDetalle, setLoadingDetalle] = useState<boolean>(false);
  const [errorDetalle, setErrorDetalle] = useState<string | null>(null);

  // Estado para almacenar los pagos (para obtener método de pago)
  const [pagos, setPagos] = useState<PagoResponse[]>([]);

  // Función para cargar egresos de tipo DEBITO
  const fetchEgresos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await egresosApi.listarEgresosDebito();
      setEgresos(Array.isArray(data) ? data : []);
      setVisibleCount(itemsPerPage);
    } catch (err) {
      toast.error("Error al cargar egresos.");
      setError("Error al cargar egresos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchEgresos();
  }, [fetchEgresos]);

  // Filtrado por rango de fecha para egresos
  const filteredEgresos = useMemo(() => {
    return egresos.filter((egreso) => {
      const egresoDate = new Date(egreso.fecha);
      let valid = true;
      if (filterStartDate) {
        valid = valid && egresoDate >= new Date(filterStartDate);
      }
      if (filterEndDate) {
        valid = valid && egresoDate <= new Date(filterEndDate);
      }
      return valid;
    });
  }, [egresos, filterStartDate, filterEndDate]);

  // Subconjunto de egresos a mostrar según visibleCount
  const currentItems = useMemo(
    () => filteredEgresos.slice(0, visibleCount),
    [filteredEgresos, visibleCount]
  );

  const hasMoreEgresos = useMemo(
    () => visibleCount < filteredEgresos.length,
    [visibleCount, filteredEgresos.length]
  );

  const onLoadMoreEgresos = useCallback(() => {
    if (hasMoreEgresos) {
      setVisibleCount((prev) => prev + itemsPerPage);
    }
  }, [hasMoreEgresos]);

  const handleEliminar = async (id: number) => {
    try {
      await egresosApi.eliminarEgreso(id);
      fetchEgresos();
      toast.success("Egreso eliminado correctamente.");
    } catch (err) {
      toast.error("Error al eliminar egreso.");
    }
  };

  // Abrir y cerrar modal para egreso
  const handleAbrirModal = () => {
    setMontoEgreso(0);
    setObsEgreso("");
    setFecha(new Date().toISOString().split("T")[0]);
    setShowModal(true);
  };

  const handleCerrarModal = () => {
    setShowModal(false);
  };

  // Guardar egreso (solo DEBITO)
  const handleGuardarEgreso = async () => {
    if (!montoEgreso || montoEgreso <= 0) {
      toast.error("El monto del egreso debe ser mayor a 0.");
      return;
    }
    try {
      const nuevoEgreso: EgresoRegistroRequest = {
        fecha,
        monto: montoEgreso,
        observaciones: obsEgreso,
        metodoPagoId: 2, // ID correspondiente para DEBITO
      };
      await egresosApi.registrarEgreso(nuevoEgreso);
      toast.success("Egreso agregado correctamente.");
      setShowModal(false);
      fetchEgresos();
    } catch (err) {
      toast.error("Error al agregar egreso.");
    }
  };

  // Renderizado personalizado para las filas de la tabla de egresos
  const renderRow = (item: EgresoResponse) => [
    item.id,
    new Date(item.fecha).toLocaleDateString("es-AR"),
    `$${item.monto.toLocaleString()}`,
    item.observaciones,
  ];

  // Acciones para cada fila de egresos
  const renderActions = (item: EgresoResponse) => (
    <>
      <Boton
        onClick={() => console.log(`Editar egreso ${item.id}`)}
        className="bg-blue-500 text-white p-1 text-sm"
      >
        Editar
      </Boton>
      <Boton
        onClick={() => handleEliminar(item.id)}
        className="bg-red-500 text-white p-1 text-sm"
      >
        Eliminar
      </Boton>
    </>
  );

  // --- Sección de DetallePago por DEBITO ---
  // Función para cargar todos los DetallePagos
  const fetchDetallePagosDebito = useCallback(async () => {
    try {
      setLoadingDetalle(true);
      setErrorDetalle(null);
      const data = await pagosApi.listarDetallesPagos();
      setDetallesDebito(Array.isArray(data) ? data : []);
      setVisibleCountPagos(itemsPerPage);
    } catch (err) {
      toast.error("Error al cargar detalle de pagos.");
      setErrorDetalle("Error al cargar detalle de pagos.");
    } finally {
      setLoadingDetalle(false);
    }
  }, []);

  // Función para cargar los pagos (para obtener el método de pago)
  const fetchPagos = useCallback(async () => {
    try {
      const data = await pagosApi.listarPagos();
      setPagos(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error("Error al cargar pagos.");
    }
  }, []);

  useEffect(() => {
    fetchDetallePagosDebito();
    fetchPagos();
  }, [fetchDetallePagosDebito, fetchPagos]);

  // Filtrar los DetallePago para incluir sólo aquellos que estén cobrados,
  // cuyo pago asociado tenga método "DEBITO" y cuya fechaRegistro se encuentre
  // dentro del rango de fecha seleccionado
  const filteredDetallesDebito = useMemo(() => {
    return detallesDebito.filter((detalle) => {
      if (!detalle.cobrado || !detalle.pagoId) return false;
      const registro = new Date(detalle.fechaRegistro);
      if (filterStartDate && registro < new Date(filterStartDate)) return false;
      if (filterEndDate && registro > new Date(filterEndDate)) return false;
      const pago = pagos.find((p) => p.id === detalle.pagoId);
      return pago && pago.metodoPago?.descripcion?.toLowerCase() === "debito";
    });
  }, [detallesDebito, pagos, filterStartDate, filterEndDate]);

  // Elementos visibles de DetallePago
  const currentItemsPagos = useMemo(
    () => filteredDetallesDebito.slice(0, visibleCountPagos),
    [filteredDetallesDebito, visibleCountPagos]
  );

  const hasMorePagos = useMemo(
    () => visibleCountPagos < filteredDetallesDebito.length,
    [visibleCountPagos, filteredDetallesDebito.length]
  );

  const onLoadMorePagos = useCallback(() => {
    if (hasMorePagos) {
      setVisibleCountPagos((prev) => prev + itemsPerPage);
    }
  }, [hasMorePagos]);

  // --- Cálculos Totales ---
  // Total de aCobrar del Detalle de Pagos por Débito.
  const totalACobrar = useMemo(() => {
    return filteredDetallesDebito.reduce(
      (sum, detalle) => sum + Number(detalle.aCobrar || 0),
      0
    );
  }, [filteredDetallesDebito]);

  // Total de monto de los egresos.
  const totalEgresos = useMemo(() => {
    return filteredEgresos.reduce(
      (sum, egreso) => sum + Number(egreso.monto || 0),
      0
    );
  }, [filteredEgresos]);

  // Total final: resta entre totalACobrar y totalEgresos.
  const totalFinal = useMemo(
    () => totalACobrar - totalEgresos,
    [totalACobrar, totalEgresos]
  );

  // --- Renderizado ---
  if (loading && egresos.length === 0)
    return <div className="p-4 text-center">Cargando...</div>;
  if (error) return <div className="p-4 text-center text-red-600">{error}</div>;

  const sortedItemPagos = [...currentItemsPagos].sort((a, b) => b.id - a.id);
  const sortedItem = [...currentItems].sort((a, b) => b.id - a.id);

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Débitos</h1>

      {/* Filtros por fecha */}
      <div className="flex gap-4 mb-4">
        <div>
          <label className="block font-medium">Fecha Inicio:</label>
          <input
            type="date"
            className="border p-2 rounded-md bg-background text-foreground focus:outline-none focus:border-primary transition-colors"
            value={filterStartDate}
            onChange={(e) => {
              setFilterStartDate(e.target.value);
              setVisibleCount(itemsPerPage);
            }}
          />
        </div>
        <div>
          <label className="block font-medium">Fecha Fin:</label>
          <input
            type="date"
            className="border p-2 rounded-md bg-background text-foreground focus:outline-none focus:border-primary transition-colors"
            value={filterEndDate}
            onChange={(e) => {
              setFilterEndDate(e.target.value);
              setVisibleCount(itemsPerPage);
            }}
          />
        </div>
      </div>

      {/* Sección de Detalle de Pagos por Débito */}
      <div className="page-card mb-4 p-4 border rounded-md shadow-sm bg-background text-foreground">
        <h2 className="text-xl font-bold mb-2">Detalle Pagos por Débito</h2>
        {loadingDetalle && <div className="text-center py-2">Cargando...</div>}
        {errorDetalle && (
          <div className="text-center py-2 text-destructive">
            {errorDetalle}
          </div>
        )}
        {!loadingDetalle && !errorDetalle && (
          <Tabla
            headers={[
              "Código",
              "Alumno",
              "Concepto",
              "Valor Base",
              "Bonificación",
              "Recargo",
            ]}
            data={sortedItemPagos}
            customRender={(fila: DetallePagoResponse) => [
              fila.conceptoId || fila.id,
              fila.alumnoDisplay,
              fila.descripcionConcepto,
              fila.importeInicial,
              fila.bonificacionNombre ? fila.bonificacionNombre : "-",
              fila.recargoNombre ? fila.recargoNombre : "-",
            ]}
            emptyMessage="No hay detalle de pagos de débito"
          />
        )}
        {hasMorePagos && (
          <div className="mt-4 flex justify-center">
            <ListaConInfiniteScroll
              onLoadMore={onLoadMorePagos}
              hasMore={hasMorePagos}
              loading={loadingDetalle}
              className="mt-4"
            >
              {loadingDetalle && (
                <div className="text-center py-2">Cargando más...</div>
              )}
            </ListaConInfiniteScroll>
          </div>
        )}
      </div>
      {/* Botón para Agregar Egreso */}
      <div className="flex justify-end mb-4">
        <Boton
          onClick={handleAbrirModal}
          className="bg-green-500 text-white p-2 flex items-center gap-2 rounded"
        >
          <PlusCircle className="h-4 w-4" />
          Agregar Egreso
        </Boton>
      </div>

      {/* Tabla de Egresos */}
      <div className="border p-2 rounded-md bg-background text-foreground">
        <Tabla
          headers={["ID", "Fecha", "Monto", "Observaciones"]}
          data={sortedItem}
          customRender={renderRow}
          actions={renderActions}
          emptyMessage="No hay egresos registrados"
        />
      </div>

      {hasMoreEgresos && (
        <div className="mt-4 flex justify-center">
          <ListaConInfiniteScroll
            onLoadMore={onLoadMoreEgresos}
            hasMore={hasMoreEgresos}
            loading={loading}
            className="mt-4"
          >
            {loading && <div className="text-center py-2">Cargando más...</div>}
          </ListaConInfiniteScroll>
        </div>
      )}

      {/* Modal para Agregar Egreso */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso DEBITO</h2>
            <div className="mb-2">
              <label className="block font-medium">Fecha:</label>
              <input
                type="date"
                className="border p-2 w-full rounded-md bg-background text-foreground focus:outline-none focus:border-primary transition-colors"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full rounded-md bg-background text-foreground focus:outline-none focus:border-primary transition-colors"
                value={montoEgreso || ""}
                onChange={(e) =>
                  setMontoEgreso(Number.parseFloat(e.target.value) || 0)
                }
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full rounded-md bg-background text-foreground focus:outline-none focus:border-primary transition-colors"
                value={obsEgreso}
                onChange={(e) => setObsEgreso(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Boton onClick={handleCerrarModal}>Cancelar</Boton>
              <Boton onClick={handleGuardarEgreso}>Guardar</Boton>
            </div>
          </div>
        </div>
      )}

      {/* Sección de Totales */}
      <div className="mb-4 p-4 border rounded-md bg-background text-foreground">
        <p className="font-medium">
          Total Cobrado:{" "}
          <span className="font-bold">${totalACobrar.toLocaleString()}</span>
        </p>
        <p className="font-medium">
          Total Egresos:{" "}
          <span className="font-bold">${totalEgresos.toLocaleString()}</span>
        </p>
        <p className="font-medium">
          Neto:{" "}
          <span className="font-bold">${totalFinal.toLocaleString()}</span>
        </p>
      </div>
    </div>
  );
}
