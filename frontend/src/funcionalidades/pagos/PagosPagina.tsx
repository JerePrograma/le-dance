import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import { queryKeys } from "../../hooks/queryKeys";
import type { PagoResumenResponse } from "../../types/types";

const PAGE_SIZE = 50;

export default function PagosPagina() {
  const [alumnoId, setAlumnoId] = useState("");
  const [consultaId, setConsultaId] = useState(0);
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const pagos = useQuery({
    queryKey: queryKeys.pagos(consultaId, page, PAGE_SIZE),
    queryFn: () => pagosApi.listarPagosPorAlumno(consultaId, page, PAGE_SIZE),
    enabled: consultaId > 0,
  });
  const anulacion = useMutation({
    mutationFn: ({ pago, motivo }: { pago: PagoResumenResponse; motivo: string }) =>
      pagosApi.anularPago(pago.id, { motivo, idempotencyKey: crypto.randomUUID() }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pagos", consultaId] }),
    onError: () => toast.error("No fue posible anular el pago"),
  });

  const buscar = () => {
    const id = Number(alumnoId);
    if (Number.isInteger(id) && id > 0) { setPage(0); setConsultaId(id); }
  };
  const anular = (pago: PagoResumenResponse) => {
    const motivo = window.prompt("Motivo de la anulación");
    if (motivo) anulacion.mutate({ pago, motivo });
  };

  return <main className="table-container"><h1>Pagos</h1>
    <label>Alumno ID <input value={alumnoId} onChange={(event) => setAlumnoId(event.target.value)} /></label>
    <button onClick={buscar}>Buscar</button>
    {pagos.isError && <p>No se pudieron cargar los pagos</p>}
    <table><thead><tr><th>ID</th><th>Fecha</th><th>Monto</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>{(pagos.data?.content ?? []).map((pago) => <tr key={pago.id}><td>{pago.id}</td><td>{pago.fecha}</td>
        <td>${pago.montoRecibido}</td><td>{pago.estado}</td><td>
          <button onClick={() => pagosApi.descargarRecibo(pago.id)}>Recibo</button>
          {pago.estado === "REGISTRADO" && <button onClick={() => anular(pago)}>Anular</button>}
        </td></tr>)}</tbody></table>
    <button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>Anterior</button>
    <span> Página {page + 1} de {Math.max(pagos.data?.totalPages ?? 1, 1)} </span>
    <button disabled={!pagos.data || page + 1 >= pagos.data.totalPages} onClick={() => setPage((value) => value + 1)}>Siguiente</button>
  </main>;
}
