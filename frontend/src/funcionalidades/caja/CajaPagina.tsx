import { useState } from "react";
import { toast } from "react-toastify";
import cajaApi from "../../api/cajaApi";
import type { ResumenCajaResponse } from "../../types/types";

export default function CajaPagina() {
  const today = new Date().toISOString().slice(0, 10);
  const [desde, setDesde] = useState(today);
  const [hasta, setHasta] = useState(today);
  const [resumen, setResumen] = useState<ResumenCajaResponse>();
  const [page, setPage] = useState(0);
  const consultar = async (requestedPage = page) => {
    try { setResumen(await cajaApi.obtenerResumen(desde, hasta, requestedPage)); setPage(requestedPage); }
    catch { toast.error("No se pudo consultar la caja"); }
  };
  return <main className="table-container"><h1>Caja</h1>
    <label>Desde<input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
    <label>Hasta<input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></label>
    <button onClick={() => consultar(0)}>Consultar</button>
    {resumen && <><p>Ingresos: ${resumen.totalIngresos} — Egresos: ${resumen.totalEgresos} — Saldo: ${resumen.saldo}</p>
      <table><thead><tr><th>Fecha</th><th>Tipo</th><th>Importe</th><th>Origen</th></tr></thead>
      <tbody>{resumen.movimientos.content.map((m) => <tr key={m.id}><td>{m.fecha}</td><td>{m.tipo}</td>
        <td>${m.importe}</td><td>{m.pagoId ? `Pago ${m.pagoId}` : m.egresoId ? `Egreso ${m.egresoId}` : m.motivo}</td>
      </tr>)}</tbody></table><button disabled={page === 0} onClick={() => consultar(page - 1)}>Anterior</button><span> Página {page + 1} de {Math.max(resumen.movimientos.totalPages, 1)} </span><button disabled={page + 1 >= resumen.movimientos.totalPages} onClick={() => consultar(page + 1)}>Siguiente</button></>}
  </main>;
}
