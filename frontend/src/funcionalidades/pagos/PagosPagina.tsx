import { useState } from "react";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import type { PagoResponse } from "../../types/types";

export default function PagosPagina() {
  const [alumnoId, setAlumnoId] = useState("");
  const [pagos, setPagos] = useState<PagoResponse[]>([]);

  const buscar = async () => {
    const id = Number(alumnoId);
    if (!Number.isInteger(id) || id <= 0) return;
    try { setPagos(await pagosApi.listarPagosPorAlumno(id)); }
    catch { toast.error("No se pudieron cargar los pagos"); }
  };

  const anular = async (pago: PagoResponse) => {
    const motivo = window.prompt("Motivo de la anulación");
    if (!motivo) return;
    try {
      await pagosApi.anularPago(pago.id, { motivo, idempotencyKey: crypto.randomUUID() });
      await buscar();
    } catch { toast.error("No fue posible anular el pago"); }
  };

  return <main className="table-container"><h1>Pagos</h1>
    <label>Alumno ID <input value={alumnoId} onChange={(e) => setAlumnoId(e.target.value)} /></label>
    <button onClick={buscar}>Buscar</button>
    <table><thead><tr><th>ID</th><th>Fecha</th><th>Monto</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>{pagos.map((pago) => <tr key={pago.id}><td>{pago.id}</td><td>{pago.fecha}</td>
        <td>${pago.montoRecibido}</td><td>{pago.estado}</td><td>
          <button onClick={() => pagosApi.descargarRecibo(pago.id)}>Recibo</button>
          {pago.estado === "REGISTRADO" && <button onClick={() => anular(pago)}>Anular</button>}
        </td></tr>)}</tbody></table>
  </main>;
}
