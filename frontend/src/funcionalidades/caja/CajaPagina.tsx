import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import cajaApi from "../../api/cajaApi";
import { queryKeys } from "../../hooks/queryKeys";

const PAGE_SIZE = 50;

export default function CajaPagina() {
  const today = new Date().toISOString().slice(0, 10);
  const [desde, setDesde] = useState(today);
  const [hasta, setHasta] = useState(today);
  const [consulta, setConsulta] = useState<{ desde: string; hasta: string; page: number }>();
  const resumen = useQuery({
    queryKey: queryKeys.caja(consulta?.desde ?? "", consulta?.hasta ?? "", consulta?.page ?? 0, PAGE_SIZE),
    queryFn: () => cajaApi.obtenerResumen(consulta!.desde, consulta!.hasta, consulta!.page, PAGE_SIZE),
    enabled: Boolean(consulta),
  });
  const consultar = (page: number) => setConsulta({ desde, hasta, page });
  const cambiarPagina = (page: number) => setConsulta((actual) => actual && { ...actual, page });
  return <main className="table-container"><h1>Caja</h1>
    <label>Desde<input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} /></label>
    <label>Hasta<input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} /></label>
    <button onClick={() => consultar(0)}>Consultar</button>
    {resumen.isError && <p>No se pudo consultar la caja</p>}
    {resumen.data && <><p>Ingresos efectivos: ${resumen.data.totalIngresos} — Egresos efectivos: ${resumen.data.totalEgresos} — Saldo: ${resumen.data.saldo}</p>
      <p>Ajustes +${resumen.data.ajustesIngreso} / -${resumen.data.ajustesEgreso}; reversos de ingresos ${resumen.data.reversosIngreso}; reversos de egresos ${resumen.data.reversosEgreso}</p>
      <table><thead><tr><th>Fecha</th><th>Tipo</th><th>Importe</th><th>Origen</th></tr></thead>
      <tbody>{resumen.data.movimientos.content.map((m) => <tr key={m.id}><td>{m.fecha}</td><td>{m.tipo}</td>
        <td>${m.importe}</td><td>{m.pagoId ? `Pago ${m.pagoId}` : m.egresoId ? `Egreso ${m.egresoId}` : m.motivo}</td>
      </tr>)}</tbody></table><button disabled={!consulta || consulta.page === 0} onClick={() => cambiarPagina(consulta!.page - 1)}>Anterior</button><span> Página {(consulta?.page ?? 0) + 1} de {Math.max(resumen.data.movimientos.totalPages, 1)} </span><button disabled={!consulta || consulta.page + 1 >= resumen.data.movimientos.totalPages} onClick={() => cambiarPagina(consulta!.page + 1)}>Siguiente</button></>}
  </main>;
}
