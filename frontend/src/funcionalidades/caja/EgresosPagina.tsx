import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "react-toastify";
import egresosApi from "../../api/egresosApi";
import metodosPagoApi from "../../api/metodosPagoApi";
import { queryKeys } from "../../hooks/queryKeys";

const PAGE_SIZE = 50;

export default function EgresosPagina() {
  const [monto, setMonto] = useState("");
  const [metodoPagoId, setMetodoPagoId] = useState(0);
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const egresos = useQuery({ queryKey: queryKeys.egresos(page, PAGE_SIZE), queryFn: () => egresosApi.listarEgresos(page, PAGE_SIZE) });
  const metodos = useQuery({ queryKey: queryKeys.metodosPago, queryFn: metodosPagoApi.listarMetodosPago });
  const alta = useMutation({
    mutationFn: () => egresosApi.registrarEgreso({ monto, metodoPagoId, idempotencyKey: crypto.randomUUID() }),
    onSuccess: async () => {
      setMonto("");
      await queryClient.invalidateQueries({ queryKey: ["egresos"] });
    },
    onError: () => toast.error("No se pudo registrar el egreso"),
  });

  return <main className="table-container"><h1>Egresos</h1>
    <input inputMode="decimal" placeholder="0.00" value={monto} onChange={(event) => setMonto(event.target.value)} />
    <select value={metodoPagoId} onChange={(event) => setMetodoPagoId(Number(event.target.value))}>
      <option value={0}>Método</option>
      {(metodos.data ?? []).filter((metodo) => metodo.activo)
        .map((metodo) => <option key={metodo.id} value={metodo.id}>{metodo.descripcion}</option>)}
    </select>
    <button onClick={() => alta.mutate()} disabled={alta.isPending}>Registrar</button>
    {egresos.isError && <p>No se pudieron cargar los egresos</p>}
    <table><thead><tr><th>Fecha</th><th>Monto</th><th>Estado</th></tr></thead>
      <tbody>{(egresos.data?.content ?? []).map((egreso) => <tr key={egreso.id}>
        <td>{egreso.fecha}</td><td>${egreso.monto}</td><td>{egreso.estado}</td>
      </tr>)}</tbody></table>
    <button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Anterior</button>
    <span> Página {page + 1} de {Math.max(egresos.data?.totalPages ?? 1, 1)} </span>
    <button disabled={!egresos.data || page + 1 >= egresos.data.totalPages} onClick={() => setPage((value) => value + 1)}>Siguiente</button>
  </main>;
}
