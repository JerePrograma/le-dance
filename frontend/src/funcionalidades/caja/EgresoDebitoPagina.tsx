"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { PlusCircle } from "lucide-react";
import { toast } from "react-toastify";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import ListaConInfiniteScroll from "../../componentes/comunes/ListaConInfiniteScroll";
import egresosApi from "../../api/egresosApi";
import pagosApi from "../../api/pagosApi";
import detallesPagoApi from "../../api/detallesPagoApi";
import type {
  EgresoResponse,
  EgresoRegistroRequest,
  DetallePagoResponse,
  PagoResponse,
  AlumnoResponse,
} from "../../types/types";

type DebitoGroupRow = {
  codigo: number; // pagoId
  alumnoNombre: string;
  conceptos: string[];
  totalDetalles: number; // sumatoria de ACobrar de los detalles
  recargo: number; // recargo del método de pago
  total: number; // totalDetalles + recargo
  fecha: string; // última fechaRegistro del grupo
};

export default function EgresosDebitoPagina() {
  const itemsPerPage = 25;

  // Fecha por defecto en AR (YYYY-MM-DD)
  const defaultDate = new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  });

  /* ----------------- Estados EGRESOS ----------------- */
  const [egresos, setEgresos] = useState<EgresoResponse[]>([]);
  const [visibleCountEgresos, setVisibleCountEgresos] =
    useState<number>(itemsPerPage);
  const [loadingEgresos, setLoadingEgresos] = useState<boolean>(false);
  const [errorEgresos, setErrorEgresos] = useState<string | null>(null);

  /* ----------------- Estados DETALLE PAGO (DEBITO) ----------------- */
  const [detallesDebito, setDetallesDebito] = useState<DetallePagoResponse[]>(
    []
  );
  const [visibleCountPagos, setVisibleCountPagos] =
    useState<number>(itemsPerPage);
  const [loadingDetalle, setLoadingDetalle] = useState<boolean>(false);
  const [errorDetalle, setErrorDetalle] = useState<string | null>(null);
  const [pagos, setPagos] = useState<PagoResponse[]>([]); // para método de pago

  /* ----------------- Filtros de fecha ----------------- */
  const [filterStartDate, setFilterStartDate] = useState<string>(defaultDate);
  const [filterEndDate, setFilterEndDate] = useState<string>(defaultDate);

  /* ----------------- Modal EGRESO ----------------- */
  const [showModal, setShowModal] = useState<boolean>(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");
  const [fecha, setFecha] = useState<string>(
    new Date().toISOString().split("T")[0]
  );

  /* ----------------- Fetchers ----------------- */
  const fetchEgresos = useCallback(async () => {
    try {
      setLoadingEgresos(true);
      setErrorEgresos(null);
      const data = await egresosApi.listarEgresosDebito();
      setEgresos(Array.isArray(data) ? data : []);
      setVisibleCountEgresos(itemsPerPage);
    } catch {
      toast.error("Error al cargar egresos.");
      setErrorEgresos("Error al cargar egresos.");
    } finally {
      setLoadingEgresos(false);
    }
  }, []);

  const fetchDetallePagosDebito = useCallback(async () => {
    try {
      setLoadingDetalle(true);
      setErrorDetalle(null);
      const data = await detallesPagoApi.listarDetallesPagosFecha({
        fechaDesde: filterStartDate || undefined,
        fechaHasta: filterEndDate || undefined,
      });
      setDetallesDebito(Array.isArray(data) ? data : []);
      setVisibleCountPagos(itemsPerPage);
    } catch {
      toast.error("Error al cargar detalle de pagos.");
      setErrorDetalle("Error al cargar detalle de pagos.");
    } finally {
      setLoadingDetalle(false);
    }
  }, [filterStartDate, filterEndDate]);

  const fetchPagos = useCallback(async () => {
    try {
      const data = await pagosApi.listarPagos();
      setPagos(Array.isArray(data) ? data : []);
    } catch {
      toast.error("Error al cargar pagos.");
    }
  }, []);

  useEffect(() => {
    fetchEgresos();
  }, [fetchEgresos]);
  useEffect(() => {
    fetchDetallePagosDebito();
    fetchPagos();
  }, [fetchDetallePagosDebito, fetchPagos]);

  /* ----------------- Filtrado EGRESOS ----------------- */
  const filteredEgresos = useMemo(() => {
    return egresos.filter((egreso) => {
      const egresoDate = new Date(egreso.fecha);
      let valid = true;
      if (filterStartDate)
        valid = valid && egresoDate >= new Date(filterStartDate);
      if (filterEndDate) valid = valid && egresoDate <= new Date(filterEndDate);
      return valid;
    });
  }, [egresos, filterStartDate, filterEndDate]);

  const currentEgresos = useMemo(
    () => filteredEgresos.slice(0, visibleCountEgresos),
    [filteredEgresos, visibleCountEgresos]
  );

  const hasMoreEgresos = useMemo(
    () => visibleCountEgresos < filteredEgresos.length,
    [visibleCountEgresos, filteredEgresos.length]
  );

  const onLoadMoreEgresos = useCallback(() => {
    if (hasMoreEgresos) setVisibleCountEgresos((prev) => prev + itemsPerPage);
  }, [hasMoreEgresos]);

  /* ----------------- Filtrado DetallePago -> sólo débitos ----------------- */
  const filteredDetallesDebito = useMemo(() => {
    return detallesDebito.filter((detalle) => {
      if (!detalle.pagoId) return false;
      const pago = pagos.find((p) => p.id === detalle.pagoId);
      return pago && pago.metodoPago?.descripcion?.toLowerCase() === "debito";
    });
  }, [detallesDebito, pagos]);

  const defaultAlumno: AlumnoResponse = {
    id: 0,
    nombre: "",
    apellido: "",
    fechaNacimiento: defaultDate,
    fechaIncorporacion: defaultDate,
    edad: 0,
    celular1: "",
    celular2: "",
    email: "",
    email2: "",
    documento: "",
    fechaDeBaja: null,
    deudaPendiente: false,
    nombrePadres: "",
    autorizadoParaSalirSolo: false,
    activo: false,
    otrasNotas: "",
    cuotaTotal: 0,
    inscripciones: [],
    creditoAcumulado: 0,
  };

  /* ----------------- AGRUPACIÓN por pagoId ----------------- */
  const debitosAgrupados: DebitoGroupRow[] = useMemo(() => {
    const groups = new Map<
      number,
      {
        alumno: AlumnoResponse;
        conceptos: string[];
        sum: number;
        recargo: number;
        fecha?: string;
      }
    >();

    filteredDetallesDebito.forEach((detalle) => {
      if (!detalle.pagoId) return;

      const pago = pagos.find((p) => p.id === detalle.pagoId);
      const rawRecargo = (pago?.metodoPago as any)?.recargo;
      const recargo =
        typeof rawRecargo === "number" ? rawRecargo : Number(rawRecargo ?? 0);

      const g = groups.get(detalle.pagoId) ?? {
        alumno: detalle.alumno || defaultAlumno,
        conceptos: [],
        sum: 0,
        recargo,
        fecha: detalle.fechaRegistro,
      };

      g.conceptos.push(detalle.descripcionConcepto || "");
      g.sum += Number(detalle.ACobrar || 0);
      g.recargo = recargo; // si cambia entre lecturas, se toma la última
      if (
        detalle.fechaRegistro &&
        (!g.fecha || new Date(detalle.fechaRegistro) > new Date(g.fecha))
      ) {
        g.fecha = detalle.fechaRegistro;
      }
      groups.set(detalle.pagoId, g);
    });

    return Array.from(groups.entries())
      .map(([pagoId, g]) => ({
        codigo: pagoId,
        alumnoNombre:
          `${g.alumno?.nombre ?? ""} ${g.alumno?.apellido ?? ""}`.trim() || "-",
        conceptos: g.conceptos,
        totalDetalles: g.sum,
        recargo: g.recargo,
        total: g.sum + g.recargo,
        fecha: g.fecha ?? new Date().toISOString(),
      }))
      .sort((a, b) => b.codigo - a.codigo); // orden por código desc
  }, [filteredDetallesDebito, pagos]);

  // Paginación sobre agrupados
  const currentPagos = useMemo(
    () => debitosAgrupados.slice(0, visibleCountPagos),
    [debitosAgrupados, visibleCountPagos]
  );

  const hasMorePagos = useMemo(
    () => visibleCountPagos < debitosAgrupados.length,
    [visibleCountPagos, debitosAgrupados.length]
  );

  const onLoadMorePagos = useCallback(() => {
    if (hasMorePagos) setVisibleCountPagos((prev) => prev + itemsPerPage);
  }, [hasMorePagos]);

  /* ----------------- Acciones EGRESOS ----------------- */
  const handleEliminarEgreso = async (id: number) => {
    try {
      await egresosApi.eliminarEgreso(id);
      fetchEgresos();
      toast.success("Egreso eliminado correctamente.");
    } catch {
      toast.error("Error al eliminar egreso.");
    }
  };

  const renderActionsEgreso = (item: EgresoResponse) => (
    <>
      <Boton
        onClick={() => console.log(`Editar egreso ${item.id}`)}
        className="bg-blue-500 text-white p-1 text-sm"
      >
        Editar
      </Boton>
      <Boton
        onClick={() => handleEliminarEgreso(item.id)}
        className="bg-red-500 text-white p-1 text-sm"
      >
        Eliminar
      </Boton>
    </>
  );

  /* ----------------- Modal EGRESO ----------------- */
  const handleAbrirModal = () => {
    setMontoEgreso(0);
    setObsEgreso("");
    setFecha(defaultDate);
    setShowModal(true);
  };
  const handleCerrarModal = () => setShowModal(false);

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
        metodoPagoId: 0,
        metodoPagoDescripcion: "DEBITO",
      };
      await egresosApi.registrarEgreso(nuevoEgreso);
      toast.success("Egreso agregado correctamente.");
      setShowModal(false);
      fetchEgresos();
    } catch {
      toast.error("Error al agregar egreso.");
    }
  };

  /* ----------------- Totales ----------------- */
  const totalCobrado = useMemo(
    () => debitosAgrupados.reduce((sum, r) => sum + r.total, 0),
    [debitosAgrupados]
  );

  const totalEgresos = useMemo(
    () =>
      filteredEgresos.reduce(
        (sum, egreso) => sum + Number(egreso.monto || 0),
        0
      ),
    [filteredEgresos]
  );

  const neto = useMemo(
    () => totalCobrado - totalEgresos,
    [totalCobrado, totalEgresos]
  );

  /* ----------------- Render ----------------- */
  if (loadingEgresos && egresos.length === 0)
    return <div className="p-4 text-center">Cargando...</div>;
  if (errorEgresos)
    return <div className="p-4 text-center text-red-600">{errorEgresos}</div>;

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Débitos</h1>

      {/* Filtros de Fecha */}
      <div className="flex gap-4 mb-4">
        <div>
          <label className="block font-medium">Fecha Inicio:</label>
          <input
            type="date"
            className="border p-2 rounded-md"
            value={filterStartDate}
            onChange={(e) => {
              setFilterStartDate(e.target.value);
              setVisibleCountEgresos(itemsPerPage);
            }}
          />
        </div>
        <div>
          <label className="block font-medium">Fecha Fin:</label>
          <input
            type="date"
            className="border p-2 rounded-md"
            value={filterEndDate}
            onChange={(e) => {
              setFilterEndDate(e.target.value);
              setVisibleCountEgresos(itemsPerPage);
            }}
          />
        </div>
      </div>

      {/* Ingresos de Pagos por Débito (agrupado por pagoId) */}
      <div className="page-card mb-4 p-4 border rounded-md shadow-sm">
        <h2 className="text-xl font-bold mb-2">Ingresos de Pagos por Débito</h2>
        {loadingDetalle && <div className="text-center py-2">Cargando...</div>}
        {errorDetalle && (
          <div className="text-center py-2 text-red-600">{errorDetalle}</div>
        )}
        {!loadingDetalle && !errorDetalle && (
          <Tabla
            headers={[
              "Código",
              "Alumno",
              "Conceptos",
              "Detalles $",
              "Recargo $",
              "Total $",
              "Cant. Det.",
            ]}
            data={currentPagos}
            customRender={(fila: DebitoGroupRow) => {
              const conceptosTxt = fila.conceptos.filter(Boolean).join(" • ");
              return [
                fila.codigo,
                fila.alumnoNombre,
                <span
                  key="conceptos"
                  className="block max-w-[280px] truncate"
                  title={conceptosTxt}
                >
                  {conceptosTxt}
                </span>,
                `$${fila.totalDetalles.toLocaleString()}`,
                `$${fila.recargo.toLocaleString()}`,
                `$${fila.total.toLocaleString()}`,
                fila.conceptos.length,
              ];
            }}
            emptyMessage="No hay pagos por débito"
          />
        )}
        {hasMorePagos && (
          <div className="mt-4 flex justify-center">
            <ListaConInfiniteScroll
              onLoadMore={onLoadMorePagos}
              hasMore={hasMorePagos}
              loading={loadingDetalle}
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
      <div className="border p-2 rounded-md">
        <Tabla
          headers={["ID", "Fecha", "Monto", "Observaciones"]}
          data={[...currentEgresos].sort((a, b) => b.id - a.id)}
          customRender={(item: EgresoResponse) => [
            item.id,
            new Date(item.fecha).toLocaleDateString("es-AR"),
            `$${item.monto.toLocaleString()}`,
            item.observaciones,
          ]}
          actions={renderActionsEgreso}
          emptyMessage="No hay egresos registrados"
        />
      </div>
      {hasMoreEgresos && (
        <div className="mt-4 flex justify-center">
          <ListaConInfiniteScroll
            onLoadMore={onLoadMoreEgresos}
            hasMore={hasMoreEgresos}
            loading={loadingEgresos}
          >
            {loadingEgresos && (
              <div className="text-center py-2">Cargando más...</div>
            )}
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
                className="border p-2 w-full rounded-md"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full rounded-md"
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
                className="border p-2 w-full rounded-md"
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

      {/* Totales */}
      <div className="mb-4 p-4 border rounded-md">
        <p className="font-medium">
          Total Cobrado:{" "}
          <span className="font-bold">${totalCobrado.toLocaleString()}</span>
        </p>
        <p className="font-medium">
          Total Egresos:{" "}
          <span className="font-bold">${totalEgresos.toLocaleString()}</span>
        </p>
        <p className="font-medium">
          Neto: <span className="font-bold">${neto.toLocaleString()}</span>
        </p>
      </div>
    </div>
  );
}
