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

export default function EgresosDebitoPagina() {
  const itemsPerPage = 25;

  // Establecemos por defecto la fecha actual (en GMT–3) en formato YYYY-MM-DD.
  const defaultDate = new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  });

  /* ----------------- Estados para EGRESOS ----------------- */
  const [egresos, setEgresos] = useState<EgresoResponse[]>([]);
  const [visibleCountEgresos, setVisibleCountEgresos] =
    useState<number>(itemsPerPage);
  const [loadingEgresos, setLoadingEgresos] = useState<boolean>(false);
  const [errorEgresos, setErrorEgresos] = useState<string | null>(null);

  /* ----------------- Estados para DETALLE PAGO (DEBITO) ----------------- */
  const [detallesDebito, setDetallesDebito] = useState<DetallePagoResponse[]>(
    []
  );
  const [visibleCountPagos, setVisibleCountPagos] =
    useState<number>(itemsPerPage);
  const [loadingDetalle, setLoadingDetalle] = useState<boolean>(false);
  const [errorDetalle, setErrorDetalle] = useState<string | null>(null);
  // Pagos se usa para filtrar por método de pago
  const [pagos, setPagos] = useState<PagoResponse[]>([]);

  /* ----------------- Estados para filtros de fecha ----------------- */
  const [filterStartDate, setFilterStartDate] = useState<string>(defaultDate);
  const [filterEndDate, setFilterEndDate] = useState<string>(defaultDate);

  /* ----------------- Estados para Modal de EGRESO ----------------- */
  const [showModal, setShowModal] = useState<boolean>(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");
  const [fecha, setFecha] = useState<string>(
    new Date().toISOString().split("T")[0]
  );

  /* ----------------- Funciones para cargar datos ----------------- */

  // Cargar egresos de tipo DEBITO
  const fetchEgresos = useCallback(async () => {
    try {
      setLoadingEgresos(true);
      setErrorEgresos(null);
      const data = await egresosApi.listarEgresosDebito();
      setEgresos(Array.isArray(data) ? data : []);
      setVisibleCountEgresos(itemsPerPage);
    } catch (err) {
      toast.error("Error al cargar egresos.");
      setErrorEgresos("Error al cargar egresos.");
    } finally {
      setLoadingEgresos(false);
    }
  }, []);

  // Cargar DetallePago filtrados en el backend por fecha
  const fetchDetallePagosDebito = useCallback(async () => {
    try {
      setLoadingDetalle(true);
      setErrorDetalle(null);
      // Se pasan como query params las fechas seleccionadas
      const data = await detallesPagoApi.listarDetallesPagosFecha({
        fechaDesde: filterStartDate || undefined,
        fechaHasta: filterEndDate || undefined,
      });
      setDetallesDebito(Array.isArray(data) ? data : []);
      setVisibleCountPagos(itemsPerPage);
    } catch (err) {
      toast.error("Error al cargar detalle de pagos.");
      setErrorDetalle("Error al cargar detalle de pagos.");
    } finally {
      setLoadingDetalle(false);
    }
  }, [filterStartDate, filterEndDate]);

  // Cargar pagos (para obtener método de pago y demás info)
  const fetchPagos = useCallback(async () => {
    try {
      const data = await pagosApi.listarPagos();
      setPagos(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error("Error al cargar pagos.");
    }
  }, []);

  // Ejecutar cargas al montar o cuando cambien las fechas de filtro
  useEffect(() => {
    fetchEgresos();
  }, [fetchEgresos]);

  useEffect(() => {
    fetchDetallePagosDebito();
    fetchPagos();
  }, [fetchDetallePagosDebito, fetchPagos]);

  /* ----------------- Filtrado de EGRESOS ----------------- */
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

  const currentEgresos = useMemo(
    () => filteredEgresos.slice(0, visibleCountEgresos),
    [filteredEgresos, visibleCountEgresos]
  );

  const hasMoreEgresos = useMemo(
    () => visibleCountEgresos < filteredEgresos.length,
    [visibleCountEgresos, filteredEgresos.length]
  );

  const onLoadMoreEgresos = useCallback(() => {
    if (hasMoreEgresos) {
      setVisibleCountEgresos((prev) => prev + itemsPerPage);
    }
  }, [hasMoreEgresos]);

  /* ----------------- Filtrado de DetallePago (DEBITO) ----------------- */
  // Aquí se muestran todos los detalles que pertenezcan a pagos con método "debito"
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

  /* ----------------- Agrupación y Agregado por Pago Único ----------------- */
  // Para cada pago único, se agrega un item "Pago por débito" que muestra el valor del recargo
  const aggregatedItems = useMemo(() => {
    const groups = new Map<number, DetallePagoResponse[]>();
    filteredDetallesDebito.forEach((detalle) => {
      if (detalle.pagoId != null) {
        const group = groups.get(detalle.pagoId) || [];
        group.push(detalle);
        groups.set(detalle.pagoId, group);
      }
    });

    const result: DetallePagoResponse[] = [];
    groups.forEach((_grupo, pagoId) => {
      const pago = pagos.find((p) => p.id === pagoId);
      if (
        pago &&
        pago.metodoPago &&
        typeof pago.metodoPago.recargo === "number"
      ) {
        const totalRecargo = pago.metodoPago.recargo;
        const aggregatedItem: DetallePagoResponse = {
          id: 0, // id sintético para evitar colisiones
          version: 1,
          descripcionConcepto: "Pago por débito",
          cuotaOCantidad: "",
          valorBase: 0,
          bonificacionId: null,
          bonificacionNombre: "",
          recargoId: null,
          recargoNombre: "",
          ACobrar: totalRecargo,
          cobrado: true,
          conceptoId: null,
          subConceptoId: null,
          mensualidadId: null,
          matriculaId: null,
          stockId: null,
          importeInicial: 0,
          importePendiente: 0,
          tipo: "",
          fechaRegistro: new Date().toISOString(),
          pagoId: pagoId,
          tieneRecargo: false,
          estadoPago: "",
          removido: undefined,
          alumno: defaultAlumno,
        };
        result.push(aggregatedItem);
      }
    });
    return result;
  }, [filteredDetallesDebito, pagos]);

  // Combinar los detalles filtrados con los items agregados
  const finalItems = useMemo(() => {
    return [...filteredDetallesDebito, ...aggregatedItems];
  }, [filteredDetallesDebito, aggregatedItems]);

  const sortedFinalItems = useMemo(() => {
    return [...finalItems].sort((a, b) => b.id - a.id);
  }, [finalItems]);

  const currentPagos = useMemo(
    () => sortedFinalItems.slice(0, visibleCountPagos),
    [sortedFinalItems, visibleCountPagos]
  );

  const hasMorePagos = useMemo(
    () => visibleCountPagos < sortedFinalItems.length,
    [visibleCountPagos, sortedFinalItems.length]
  );

  const onLoadMorePagos = useCallback(() => {
    if (hasMorePagos) {
      setVisibleCountPagos((prev) => prev + itemsPerPage);
    }
  }, [hasMorePagos]);

  /* ----------------- Acciones para EGRESOS ----------------- */
  const handleEliminarEgreso = async (id: number) => {
    try {
      await egresosApi.eliminarEgreso(id);
      fetchEgresos();
      toast.success("Egreso eliminado correctamente.");
    } catch (err) {
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

  /* ----------------- Acciones para Modal de EGRESO ----------------- */
  const handleAbrirModal = () => {
    setMontoEgreso(0);
    setObsEgreso("");
    setFecha(defaultDate);
    setShowModal(true);
  };

  const handleCerrarModal = () => {
    setShowModal(false);
  };

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
        metodoPagoDescripcion: "DEBITO"
      };
      await egresosApi.registrarEgreso(nuevoEgreso);
      toast.success("Egreso agregado correctamente.");
      setShowModal(false);
      fetchEgresos();
    } catch (err) {
      toast.error("Error al agregar egreso.");
    }
  };

  /* ----------------- Cálculos Totales ----------------- */
  // Total Cobrado: suma de todos los valores de ACobrar (incluyendo los items agregados)
  const totalCobrado = useMemo(() => {
    return sortedFinalItems.reduce(
      (sum, item) => sum + Number(item.ACobrar || 0),
      0
    );
  }, [sortedFinalItems]);

  // Total Egresos: suma de todos los montos de los egresos
  const totalEgresos = useMemo(() => {
    return filteredEgresos.reduce(
      (sum, egreso) => sum + Number(egreso.monto || 0),
      0
    );
  }, [filteredEgresos]);

  // Neto: Total Cobrado - Total Egresos
  const neto = useMemo(
    () => totalCobrado - totalEgresos,
    [totalCobrado, totalEgresos]
  );

  /* ----------------- Renderizado ----------------- */
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

      {/* Sección de Detalle de Pagos por Débito */}
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
              "Concepto",
              "Valor a Cobrar",
              "Bonificación",
              "Recargo",
            ]}
            data={currentPagos}
            customRender={(fila: DetallePagoResponse) => [
              fila.id,
              fila.alumno.nombre + " " + fila.alumno.apellido || "-",
              fila.descripcionConcepto,
              fila.ACobrar,
              fila.bonificacionNombre || "-",
              fila.recargoNombre || "-",
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
          data={currentEgresos.sort((a, b) => b.id - a.id)}
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

      {/* Sección de Totales */}
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
