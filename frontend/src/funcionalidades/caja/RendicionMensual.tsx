"use client";

import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import { useAuth } from "../../hooks/context/authContext";

// Se incluyen los nuevos campos según lo requerido (asegúrate de que el backend los envíe)
interface MetodoPago {
  id: number;
  descripcion: string;
}

interface PagoDelDia {
  id: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
  };
  observaciones: string;
  monto: number;
  importeInicial: number;
  importePendiente: number;
  metodoPago?: MetodoPago | null;
  usuarioId: number;
}

interface EgresoDelDia {
  id: number;
  fecha: string;
  monto: number;
  observaciones?: string;
}

export interface CajaDetalleDTO {
  pagosDelDia: PagoDelDia[];
  egresosDelDia: EgresoDelDia[];
}

export interface EgresoRegistroRequest {
  id?: number;
  fecha: string;
  monto: number;
  observaciones?: string;
  metodoPagoId: number;
  metodoPagoDescripcion: string;
}

const RendicionMensual: React.FC = () => {
  const { user } = useAuth();
  const currentUserId = user?.id || 0;

  const mesAuto = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
    year: "numeric",
    month: "2-digit",
  }).format(new Date()); // Ejemplo: "2025-04"
  const [mes, setMes] = useState<string>(mesAuto);

  const [data, setData] = useState<CajaDetalleDTO | null>(null);
  const [loading, setLoading] = useState(false);

  const [filtroPago, setFiltroPago] = useState<"mis" | "todos">("mis");

  const getLastDayOfMonth = (mes: string): string => {
    const [year, month] = mes.split("-").map(Number);
    const date = new Date(year, month, 0);
    const day = date.getDate().toString().padStart(2, "0");
    return day;
  };

  const handleVer = async () => {
    try {
      setLoading(true);
      // Calcular primer día y último día del mes seleccionado.
      const startDate = mes + "-01";
      const lastDay = getLastDayOfMonth(mes);
      const endDate = mes + "-" + lastDay;

      const detalle = await cajaApi.obtenerCajaMes(startDate, endDate);
      const mappedDetalle: CajaDetalleDTO = {
        ...detalle,
        pagosDelDia: detalle.pagosDelDia.map((p: any) => ({
          ...p,
          alumno: p.alumno ?? { id: 0, nombre: "", apellido: "" },
          // Aseguramos que si no hay datos, se asigna 0
          importeInicial: p.importeInicial ?? 0,
          importePendiente: p.importePendiente ?? 0,
        })),
        egresosDelDia: detalle.egresosDelDia.map((egreso: any) => ({
          ...egreso,
          observaciones: egreso.observaciones ?? "",
        })),
      };
      setData(mappedDetalle);
    } catch (err) {
      toast.error("Error al consultar la caja del mes.");
    } finally {
      setLoading(false);
    }
  };

  // Usamos los pagos del backend y filtramos los que tengan monto, importeInicial e importePendiente 0.
  const pagos: PagoDelDia[] = data?.pagosDelDia || [];
  const pagosValidos = pagos.filter(
    (p) =>
      !(p.monto === 0 && p.importeInicial === 0 && p.importePendiente === 0)
  );
  const sortedPagos = [...pagosValidos].sort((a, b) => b.id - a.id);

  const pagosFiltradosPorUsuario =
    filtroPago === "mis"
      ? sortedPagos.filter((p) => p.usuarioId === currentUserId)
      : sortedPagos;

  const totalEfectivo = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "EFECTIVO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalDebito = pagosFiltradosPorUsuario
    .filter((p) => p.metodoPago?.descripcion?.toUpperCase() === "DEBITO")
    .reduce((sum, p) => sum + p.monto, 0);

  const totalCobrado = totalEfectivo + totalDebito;

  const egresos: EgresoDelDia[] = data?.egresosDelDia || [];
  const sortedEgresos = [...egresos].sort((a, b) => b.id - a.id);
  const totalEgresos = egresos.reduce((sum, e) => sum + e.monto, 0);

  const handleImprimir = async () => {
    try {
      const startDate = mes + "-01";
      const lastDay = getLastDayOfMonth(mes);
      const endDate = mes + "-" + lastDay;

      const pdfBlob = await cajaApi.imprimirRendicion(startDate, endDate);

      const url = window.URL.createObjectURL(pdfBlob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `rendicion_${startDate}_${endDate}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      toast.error("Error al imprimir la rendición.");
    }
  };

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Consulta de Caja (Mes)</h1>
      <div className="flex gap-4 items-end mb-4">
        <div>
          <label className="block font-medium">Mes:</label>
          <input
            type="month"
            className="border p-2"
            value={mes}
            onChange={(e) => setMes(e.target.value)}
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
              p.alumno ? `${p.alumno.nombre} ${p.alumno.apellido}` : "",
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
            emptyMessage="No hay pagos para el mes"
          />
        )}
      </div>

      {egresos.length > 0 && (
        <div className="border p-2 mt-4">
          <h2 className="font-semibold mb-2">Egresos del mes</h2>
          <Tabla
            headers={["ID", "Observaciones", "Monto"]}
            data={sortedEgresos}
            customRender={(e: EgresoDelDia) => [
              e.id,
              e.observaciones,
              e.monto.toLocaleString(),
            ]}
            emptyMessage="No hay egresos para el mes"
          />
        </div>
      )}

      <div className="mt-4 flex gap-2">
        <Boton
          onClick={handleImprimir}
          className="bg-pink-400 text-white p-2 rounded"
        >
          Imprimir
        </Boton>
      </div>

      {/* Totales */}
      <div className="text-right mt-2">
        <p>
          Efectivo: ${" "}
          {pagosFiltradosPorUsuario
            .filter(
              (p) => p.metodoPago?.descripcion?.toUpperCase() === "EFECTIVO"
            )
            .reduce((sum, p) => sum + p.monto, 0)
            .toLocaleString()}
        </p>
        <p>
          Débito: ${" "}
          {pagosFiltradosPorUsuario
            .filter(
              (p) => p.metodoPago?.descripcion?.toUpperCase() === "DEBITO"
            )
            .reduce((sum, p) => sum + p.monto, 0)
            .toLocaleString()}
        </p>
        <p>Total neto: $ {(totalCobrado - totalEgresos).toLocaleString()}</p>
        <p>Egresos en efectivo: $ {totalEgresos.toLocaleString()}</p>
        <p>
          Total efectivo: $ {(totalEfectivo - totalEgresos).toLocaleString()}
        </p>
      </div>
    </div>
  );
};

export default RendicionMensual;
