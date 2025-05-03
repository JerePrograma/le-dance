"use client";

import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";
import egresoApi from "../../api/egresosApi";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import { useAuth } from "../../hooks/context/authContext";
import { EgresoResponse } from "../../types/types";

// Interfaces (pueden moverse a un archivo .d.ts o adaptarse)
interface MetodoPago {
  id: number;
  descripcion: string; // "EFECTIVO", "DEBITO", etc.
}

interface DetallePago {
  id: number;
  ACobrar: number;
  // si quieres filtrar por estado, podrías agregar:
  // estadoPago?: "ACTIVO" | "ANULADO" | "HISTORICO";
}

interface PagoDelDia {
  id: number;
  fecha: string;
  fechaVencimiento: string;
  monto: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
  };
  observaciones: string;
  metodoPago?: MetodoPago | null;
  usuarioId: number; // Identificador del usuario que realizó el pago
  detallePagos?: DetallePago[];
}
export interface CajaDetalleDTO {
  pagosDelDia: PagoDelDia[];
  egresosDelDia: EgresoResponse[];
}

export interface EgresoRegistroRequest {
  id?: number;
  fecha: string; // Formato ISO (ej: "2025-03-17")
  monto: number;
  observaciones?: string;
  metodoPagoId: number;
  metodoPagoDescripcion: string;
}

const ConsultaCajaDiaria: React.FC = () => {
  const { user } = useAuth();
  const currentUserId = user?.id || 0;

  const fechaAuto = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
  }).format(new Date());
  const [fecha, setFecha] = useState<string>(fechaAuto);
  const [data, setData] = useState<CajaDetalleDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [filtroPago, setFiltroPago] = useState<"mis" | "todos">("mis");

  // Modal de Egreso
  const [showModalEgreso, setShowModalEgreso] = useState(false);
  const [montoEgreso, setMontoEgreso] = useState<number>(0);
  const [obsEgreso, setObsEgreso] = useState<string>("");

  const handleVer = async () => {
    try {
      setLoading(true);
      const detalle = await cajaApi.obtenerCajaDiaria(fecha);
      const mapped: CajaDetalleDTO = {
        ...detalle,
        pagosDelDia: detalle.pagosDelDia.map((p: any) => ({
          ...p,
          alumno: p.alumno ?? { id: 0, nombre: "", apellido: "" },
        })),
        egresosDelDia: detalle.egresosDelDia.map((e) => ({
          ...e,
          observaciones: e.observaciones ?? "",
          // ¡no tocamos `metodoPago`, viene ya con id/descripcion/activo/recargo!
        })),
      };
      setData(mapped);
    } catch {
      toast.error("Error al consultar la caja diaria.");
    } finally {
      setLoading(false);
    }
  };

  const handleEliminarEgreso = async (id: number) => {
    try {
      await egresoApi.eliminarEgreso(id);
      toast.success("Egreso eliminado correctamente.");
      handleVer();
    } catch {
      toast.error("Error al eliminar egreso.");
    }
  };

  // Pagos
  const pagos = data?.pagosDelDia || [];
  const pagosFiltrados = pagos
    .filter((p) => p.monto !== 0)
  const sortedPagos = [...pagosFiltrados].sort((a, b) => b.id - a.id);
  const pagosFiltradosPorUsuario =
    filtroPago === "mis"
      ? sortedPagos.filter((p) => p.usuarioId === currentUserId)
      : sortedPagos;

  const totalEfectivo = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion.toUpperCase() === "EFECTIVO")
    .reduce((sum, p) => sum + p.monto, 0);
  const totalDebito = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion.toUpperCase() === "DEBITO")
    .reduce((sum, p) => sum + p.monto, 0);
  const totalCobrado = totalEfectivo + totalDebito;

  // Egresos — solo EFECTIVO
  const egresos = data?.egresosDelDia || [];
  const egresosEnEfectivo = egresos.filter(
    (e) => e.metodoPago?.descripcion.toUpperCase() === "EFECTIVO"
  );
  const sortedEgresosEfectivo = [...egresosEnEfectivo].sort(
    (a, b) => b.id - a.id
  );
  const totalEgresosEfectivo = sortedEgresosEfectivo.reduce(
    (sum, e) => sum + e.monto,
    0
  );

  // Egresos — solo DÉBITO
  const egresosDebito = egresos.filter(
    (e) => e.metodoPago?.descripcion.toUpperCase() === "DEBITO"
  );
  const sortedEgresosDebito = [...egresosDebito].sort((a, b) => b.id - a.id);
  const totalEgresosDebito = sortedEgresosDebito.reduce(
    (sum, e) => sum + e.monto,
    0
  );

  const handleImprimir = () =>
    toast.info("Funcionalidad de imprimir no implementada");
  const handleAbrirModalEgreso = () => {
    setMontoEgreso(0);
    setObsEgreso("");
    setShowModalEgreso(true);
  };
  const handleCerrarModal = () => setShowModalEgreso(false);
  const handleGuardarEgreso = async () => {
    if (montoEgreso === 0) {
      toast.error("El monto del egreso no puede ser 0.");
      return;
    }
    try {
      await egresoApi.registrarEgreso({
        fecha,
        monto: montoEgreso,
        observaciones: obsEgreso,
        metodoPagoDescripcion: "EFECTIVO",
        metodoPagoId: 0,
      });
      toast.success("Egreso agregado correctamente.");
      setShowModalEgreso(false);
      handleVer();
    } catch {
      toast.error("Error al agregar egreso.");
    }
  };

  // --------------------------------------------------------------------------
  // Renderizado
  // --------------------------------------------------------------------------
  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Consulta de Caja</h1>

      {/* Filtros */}
      <div className="flex gap-4 items-end mb-4">
        <div>
          <label className="block font-medium">Fecha:</label>
          <input
            type="date"
            className="border p-2"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
          />
        </div>
        <div>
          <label className="block font-medium">Mostrar:</label>
          <select
            className="border p-2"
            value={filtroPago}
            onChange={(e) => setFiltroPago(e.target.value as "mis" | "todos")}
          >
            <option value="mis">
              {user ? `Pagos de ${user.nombreUsuario}` : "Mis pagos"}
            </option>
            <option value="todos">Todos</option>
          </select>
        </div>
        <Boton
          onClick={handleVer}
          className="bg-green-500 text-white p-2 rounded"
        >
          Ver
        </Boton>
      </div>

      {/* Tabla de Pagos */}
      <div className="hidden sm:block rounded-lg border h-full overflow-auto">
        {loading ? (
          <p>Cargando...</p>
        ) : (
          <Tabla
            className="table-fixed w-full"
            headers={["Recibo", "Código", "Alumno", "Observaciones", "Importe"]}
            data={pagosFiltradosPorUsuario}
            customRender={(p: PagoDelDia) => [
              p.id,
              p.alumno?.id || "",
              `${p.alumno.nombre} ${p.alumno.apellido}`,
              <div
                style={{
                  maxWidth: "30vw",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
                title={p.observaciones}
              >
                {p.observaciones}
              </div>,
              p.monto.toLocaleString(),
            ]}
            emptyMessage="No hay pagos para el día"
          />
        )}
      </div>

      {/* Egresos en EFECTIVO */}
      {sortedEgresosEfectivo.length > 0 && (
        <div className="border p-2 mt-4">
          <h2 className="font-semibold mb-2">Egresos en efectivo del día</h2>
          <Tabla
            headers={["ID", "Observaciones", "Monto", "Acciones"]}
            data={sortedEgresosEfectivo}
            customRender={(e) => [
              e.id,
              e.observaciones,
              e.monto.toLocaleString(),
              <Boton
                onClick={() => handleEliminarEgreso(e.id)}
                className="bg-red-500 text-white p-1 text-sm"
              >
                Eliminar
              </Boton>,
            ]}
            emptyMessage="No hay egresos en efectivo para el día"
          />
        </div>
      )}

      {/* Egresos en DÉBITO */}
      {sortedEgresosDebito.length > 0 && (
        <div className="border p-2 mt-4">
          <h2 className="font-semibold mb-2">Egresos en débito del día</h2>
          <Tabla
            headers={["ID", "Observaciones", "Monto", "Acciones"]}
            data={sortedEgresosDebito}
            customRender={(e) => [
              e.id,
              e.observaciones,
              e.monto.toLocaleString(),
              <Boton
                onClick={() => handleEliminarEgreso(e.id)}
                className="bg-red-500 text-white p-1 text-sm"
              >
                Eliminar
              </Boton>,
            ]}
            emptyMessage="No hay egresos en débito para el día"
          />
        </div>
      )}

      {/* Botones finales */}
      <div className="mt-4 flex gap-2">
        <Boton
          onClick={handleImprimir}
          className="bg-pink-400 text-white p-2 rounded"
        >
          Imprimir
        </Boton>
        <Boton
          onClick={handleAbrirModalEgreso}
          className="bg-yellow-400 text-white p-2 rounded"
        >
          Agregar Egreso
        </Boton>
      </div>

      {/* Totales */}
      <div className="text-right mt-2">
        <p>Efectivo: {totalEfectivo.toLocaleString()}</p>
        <p>Débito: {totalDebito.toLocaleString()}</p>
        <p>Total cobrado: {totalCobrado.toLocaleString()}</p>
        <p>Egresos en efectivo: {totalEgresosEfectivo.toLocaleString()}</p>
        <p>Egresos en débito: {totalEgresosDebito.toLocaleString()}</p>
        <p>
          Total efectivo:{" "}
          {(totalEfectivo - totalEgresosEfectivo).toLocaleString()}
        </p>
        <p>
          Total neto:{" "}
          {(
            totalCobrado -
            (totalEgresosEfectivo + totalEgresosDebito)
          ).toLocaleString()}
        </p>
      </div>

      {/* Modal Agregar Egreso */}
      {showModalEgreso && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-30">
          <div className="bg-black p-4 rounded shadow-md w-[300px]">
            <h2 className="text-xl font-bold mb-4">Nuevo Egreso</h2>
            <div className="mb-2">
              <label className="block font-medium">Monto:</label>
              <input
                type="number"
                className="border p-2 w-full"
                value={montoEgreso}
                onChange={(e) => setMontoEgreso(parseFloat(e.target.value))}
              />
            </div>
            <div className="mb-2">
              <label className="block font-medium">Observaciones:</label>
              <input
                type="text"
                className="border p-2 w-full"
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
    </div>
  );
};

export default ConsultaCajaDiaria;
