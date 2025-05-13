// src/componentes/PlanillaCajaGeneral.tsx
import React, { useState } from "react";
import cajaApi from "../../api/cajaApi";
import Tabla from "../../componentes/comunes/Tabla";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import type { CajaPlanillaDTO } from "../../types/types";

const PlanillaCajaGeneral: React.FC = () => {
  const [startDate, setStartDate] = useState(
    () => new Date().toISOString().split("T")[0]
  );
  const [endDate, setEndDate] = useState(startDate);
  const [lista, setLista] = useState<CajaPlanillaDTO[]>([]);
  const [loading, setLoading] = useState(false);

  const totalEfectivo = lista.reduce((s, x) => s + x.totalEfectivo, 0);
  const totalDebito = lista.reduce((s, x) => s + x.totalDebito, 0);
  const totalEgresos = lista.reduce((s, x) => s + x.totalEgresos, 0);
  const totalNeto = lista.reduce((s, x) => s + x.totalNeto, 0);

  const handleVer = async () => {
    try {
      setLoading(true);
      const data = await cajaApi.obtenerPlanillaGeneral(startDate, endDate);
      setLista(data);
    } catch {
      toast.error("Error al cargar Planilla de Caja General");
    } finally {
      setLoading(false);
    }
  };

  const parseDate = (d: string) => {
    const [y, m, day] = d.split("-");
    return new Date(+y, +m - 1, +day).toLocaleDateString("es-AR");
  };

  return (
    <div className="page-container p-4">
      <h1 className="text-2xl font-bold mb-4">Planilla de Caja General</h1>

      {/* Filtros */}
      <div className="flex gap-4 items-center mb-4">
        {["Desde", "Hasta"].map((lbl, i) => (
          <div key={lbl}>
            <label className="block font-medium">{lbl}</label>
            <input
              type="date"
              className="border p-2"
              value={i === 0 ? startDate : endDate}
              onChange={(e) =>
                i === 0
                  ? setStartDate(e.target.value)
                  : setEndDate(e.target.value)
              }
            />
          </div>
        ))}
        <Boton
          onClick={handleVer}
          className="bg-green-500 text-white p-2 rounded"
        >
          Ver
        </Boton>
      </div>

      {/* Tabla */}
      <div className="border p-2">
        {loading ? (
          <p>Cargando…</p>
        ) : (
          <Tabla
            headers={[
              "Fecha",
              "Rango Recibos",
              "Efectivo",
              "Transferencia",
              "Egresos",
              "Total Neto",
            ]}
            data={lista}
            customRender={(item: CajaPlanillaDTO) => [
              parseDate(item.fecha),
              item.rangoRecibos,
              item.totalEfectivo.toLocaleString(),
              item.totalDebito.toLocaleString(),
              item.totalEgresos.toLocaleString(),
              item.totalNeto.toLocaleString(),
            ]}
            emptyMessage="Sin datos para esas fechas"
          />
        )}
      </div>

      {/* Footer con totales y acciones */}
      <div className="mt-4 flex justify-between items-center">
        <div className="flex gap-2">
          <Boton
            onClick={() => toast.info("Imprimir…")}
            className="bg-pink-400 text-white p-2 rounded"
          >
            Imprimir
          </Boton>
          <Boton
            onClick={() => toast.info("Exportar…")}
            className="bg-green-400 text-white p-2 rounded"
          >
            Exportar
          </Boton>
        </div>
        <div className="text-right space-y-1">
          <p>Total Efectivo: $ {totalEfectivo.toLocaleString()}</p>
          <p>Total Transferencia: $ {totalDebito.toLocaleString()}</p>
          <p>Total Egresos: $ {totalEgresos.toLocaleString()}</p>
          <p className="font-semibold">
            Total Neto: $ {totalNeto.toLocaleString()}
          </p>
        </div>
      </div>
    </div>
  );
};

export default PlanillaCajaGeneral;
