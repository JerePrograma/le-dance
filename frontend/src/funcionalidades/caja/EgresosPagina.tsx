"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { toast } from "react-toastify";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import type { EgresoResponse, EgresoRegistroRequest } from "../../types/types";
import egresoApi from "../../api/egresosApi";

export default function EgresosPagina() {
  const defaultDate = new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  });

  /* ESTADOS */
  const [debito, setDebito] = useState<EgresoResponse[]>([]);
  const [efectivo, setEfectivo] = useState<EgresoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [filterStartDate, setFilterStartDate] = useState(defaultDate);
  const [filterEndDate, setFilterEndDate] = useState(defaultDate);

  /* MODALES */
  const [showModalDebito, setShowModalDebito] = useState(false);
  const [showModalEfectivo, setShowModalEfectivo] = useState(false);

  /* FORM: Débito */
  const [fechaDebito, setFechaDebito] = useState(defaultDate);
  const [montoDebito, setMontoDebito] = useState(0);
  const [obsDebito, setObsDebito] = useState("");

  /* FORM: Efectivo */
  const [fechaEfectivo, setFechaEfectivo] = useState(defaultDate);
  const [montoEfectivo, setMontoEfectivo] = useState(0);
  const [obsEfectivo, setObsEfectivo] = useState("");

  /* FETCH DATOS */
  const fetchDatos = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [deb, efe] = await Promise.all([
        egresoApi.listarEgresosDebito(),
        egresoApi.listarEgresosEfectivo(),
      ]);
      setDebito(deb);
      setEfectivo(efe);
    } catch {
      setError("Error al cargar egresos.");
      toast.error("Error al cargar egresos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDatos();
  }, [fetchDatos]);

  /* FILTRADOS */
  const filteredDebito = useMemo(
    () =>
      debito
        .filter((e) => {
          const d = new Date(e.fecha);
          return d >= new Date(filterStartDate) && d <= new Date(filterEndDate);
        })
        .sort((a, b) => b.id - a.id),
    [debito, filterStartDate, filterEndDate]
  );

  const filteredEfectivo = useMemo(
    () =>
      efectivo
        .filter((e) => {
          const d = new Date(e.fecha);
          return d >= new Date(filterStartDate) && d <= new Date(filterEndDate);
        })
        .sort((a, b) => b.id - a.id),
    [efectivo, filterStartDate, filterEndDate]
  );

  /* TOTALES */
  const total = (items: EgresoResponse[]) =>
    items.reduce((sum, e) => sum + e.monto, 0);

  /* GUARDAR */
  const handleGuardarDebito = async () => {
    if (montoDebito <= 0) {
      toast.error("Monto debe ser mayor a cero");
      return;
    }
    const payload: EgresoRegistroRequest = {
      fecha: fechaDebito,
      monto: montoDebito,
      observaciones: obsDebito,
      metodoPagoId: 0,
      metodoPagoDescripcion: "DEBITO",
    };
    try {
      await egresoApi.registrarEgreso(payload);
      toast.success("Egreso de débito agregado");
      fetchDatos();
      setShowModalDebito(false);
      // reset form
      setMontoDebito(0);
      setObsDebito("");
      setFechaDebito(defaultDate);
    } catch {
      toast.error("Error al registrar egreso");
    }
  };

  const handleGuardarEfectivo = async () => {
    if (montoEfectivo <= 0) {
      toast.error("Monto debe ser mayor a cero");
      return;
    }
    const payload: EgresoRegistroRequest = {
      fecha: fechaEfectivo,
      monto: montoEfectivo,
      observaciones: obsEfectivo,
      metodoPagoId: 0,
      metodoPagoDescripcion: "EFECTIVO",
    };
    try {
      await egresoApi.registrarEgreso(payload);
      toast.success("Egreso de efectivo agregado");
      fetchDatos();
      setShowModalEfectivo(false);
      // reset form
      setMontoEfectivo(0);
      setObsEfectivo("");
      setFechaEfectivo(defaultDate);
    } catch {
      toast.error("Error al registrar egreso");
    }
  };

  if (loading) return <div className="p-4 text-center">Cargando...</div>;
  if (error) return <div className="p-4 text-center text-red-600">{error}</div>;

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Egresos Débito y Efectivo</h1>

      {/* FILTROS + BOTONES */}
      <div className="flex gap-4 mb-4">
        <div>
          <label className="block font-medium">Fecha Inicio:</label>
          <input
            type="date"
            className="border p-2 rounded-md"
            value={filterStartDate}
            onChange={(e) => setFilterStartDate(e.target.value)}
          />
        </div>
        <div>
          <label className="block font-medium">Fecha Fin:</label>
          <input
            type="date"
            className="border p-2 rounded-md"
            value={filterEndDate}
            onChange={(e) => setFilterEndDate(e.target.value)}
          />
        </div>
        <div className="ml-auto flex gap-2">
          <Boton onClick={() => setShowModalDebito(true)}>
            Nuevo egreso débito
          </Boton>
          <Boton onClick={() => setShowModalEfectivo(true)}>
            Nuevo egreso efectivo
          </Boton>
        </div>
      </div>

      {/* TABLAS */}
      <div className="space-y-6">
        <div className="mt-4">
          <h2 className="font-semibold mb-2">Débito</h2>
          <Tabla
            headers={["ID", "Obs.", "Monto", "Fecha", "Acc."]}
            data={filteredDebito}
            customRender={(e) => [
              e.id,
              e.observaciones || "",
              `$${e.monto.toLocaleString()}`,
              new Date(e.fecha).toLocaleDateString("es-AR"),
              <Boton
                onClick={async () => {
                  try {
                    await egresoApi.eliminarEgreso(e.id);
                    toast.success("Egreso de débito eliminado");
                    fetchDatos();
                  } catch {
                    toast.error("Error al eliminar egreso de débito");
                  }
                }}
                className="bg-red-500 text-white p-1 text-sm"
              >
                Eliminar
              </Boton>,
            ]}
            emptyMessage="No hay egresos débito"
          />
          <p className="text-right mt-2 font-medium">
            Total débito: ${total(filteredDebito).toLocaleString()}
          </p>
        </div>

        <div className="mt-4">
          <h2 className="font-semibold mb-2">Efectivo</h2>
          <Tabla
            headers={["ID", "Obs.", "Monto", "Fecha", "Acc."]}
            data={filteredEfectivo}
            customRender={(e) => [
              e.id,
              e.observaciones || "",
              `$${e.monto.toLocaleString()}`,
              new Date(e.fecha).toLocaleDateString("es-AR"),
              <Boton
                onClick={async () => {
                  try {
                    await egresoApi.eliminarEgreso(e.id);
                    toast.success("Egreso de efectivo eliminado");
                    fetchDatos();
                  } catch {
                    toast.error("Error al eliminar egreso de efectivo");
                  }
                }}
                className="bg-red-500 text-white p-1 text-sm"
              >
                Eliminar
              </Boton>,
            ]}
            emptyMessage="No hay egresos efectivo"
          />
          <p className="text-right mt-2 font-medium">
            Total efectivo: ${total(filteredEfectivo).toLocaleString()}
          </p>
        </div>
      </div>

      {/* NETO */}
      <p className="text-right mt-4 font-medium">
        Total neto: $
        {(total(filteredDebito) + total(filteredEfectivo)).toLocaleString()}
      </p>

      {/* Modal Débito */}
      {showModalDebito && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso DÉBITO</h2>
            <div className="mb-2">
              <label className="block font-medium">Fecha:</label>
              <input
                type="date"
                className="border p-2 w-full rounded-md"
                value={fechaDebito}
                onChange={(e) => setFechaDebito(e.target.value)}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full rounded-md"
                value={montoDebito || ""}
                onChange={(e) =>
                  setMontoDebito(Number.parseFloat(e.target.value) || 0)
                }
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full rounded-md"
                value={obsDebito}
                onChange={(e) => setObsDebito(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Boton onClick={() => setShowModalDebito(false)}>Cancelar</Boton>
              <Boton onClick={handleGuardarDebito}>Guardar</Boton>
            </div>
          </div>
        </div>
      )}

      {/* Modal Efectivo */}
      {showModalEfectivo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso EFECTIVO</h2>
            <div className="mb-2">
              <label className="block font-medium">Fecha:</label>
              <input
                type="date"
                className="border p-2 w-full rounded-md"
                value={fechaEfectivo}
                onChange={(e) => setFechaEfectivo(e.target.value)}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full rounded-md"
                value={montoEfectivo || ""}
                onChange={(e) =>
                  setMontoEfectivo(Number.parseFloat(e.target.value) || 0)
                }
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full rounded-md"
                value={obsEfectivo}
                onChange={(e) => setObsEfectivo(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <Boton onClick={() => setShowModalEfectivo(false)}>
                Cancelar
              </Boton>
              <Boton onClick={handleGuardarEfectivo}>Guardar</Boton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
