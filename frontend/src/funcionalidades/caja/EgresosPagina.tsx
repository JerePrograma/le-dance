import { useEffect, useState } from "react";
import { toast } from "react-toastify";
import egresosApi from "../../api/egresosApi";
import metodosPagoApi from "../../api/metodosPagoApi";
import type { EgresoResponse, MetodoPagoResponse } from "../../types/types";

export default function EgresosPagina() {
  const [egresos, setEgresos] = useState<EgresoResponse[]>([]);
  const [metodos, setMetodos] = useState<MetodoPagoResponse[]>([]);
  const [monto, setMonto] = useState("");
  const [metodoPagoId, setMetodoPagoId] = useState(0);
  const cargar = () => egresosApi.listarEgresos().then(setEgresos);
  useEffect(() => { cargar().catch(() => toast.error("No se pudieron cargar egresos"));
    metodosPagoApi.listarMetodosPago().then(setMetodos); }, []);
  const registrar = async () => {
    try {
      await egresosApi.registrarEgreso({ monto, metodoPagoId, idempotencyKey: crypto.randomUUID() });
      setMonto(""); await cargar();
    } catch { toast.error("No se pudo registrar el egreso"); }
  };
  return <main className="table-container"><h1>Egresos</h1>
    <input inputMode="decimal" placeholder="0.00" value={monto} onChange={(e) => setMonto(e.target.value)} />
    <select value={metodoPagoId} onChange={(e) => setMetodoPagoId(Number(e.target.value))}><option value={0}>Método</option>
      {metodos.map((m) => <option key={m.id} value={m.id}>{m.descripcion}</option>)}</select>
    <button onClick={registrar}>Registrar</button>
    <table><thead><tr><th>Fecha</th><th>Monto</th><th>Estado</th></tr></thead>
      <tbody>{egresos.map((e) => <tr key={e.id}><td>{e.fecha}</td><td>${e.monto}</td><td>{e.estado}</td></tr>)}</tbody></table>
  </main>;
}
