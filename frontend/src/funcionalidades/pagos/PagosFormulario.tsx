import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "react-toastify";
import cargosApi from "../../api/cargosApi";
import metodosPagoApi from "../../api/metodosPagoApi";
import pagosApi from "../../api/pagosApi";
import type { CargoResponse, MetodoPagoResponse, Page } from "../../types/types";
import { queryKeys } from "../../hooks/queryKeys";
import { isPositiveMoney } from "../../utils/money";

export default function PagosFormulario() {
  const queryClient = useQueryClient();
  const metodos = useQuery<MetodoPagoResponse[]>({
    queryKey: queryKeys.metodosPago,
    queryFn: metodosPagoApi.listarMetodosPago,
  });
  const [alumnoId, setAlumnoId] = useState(0);
  const [metodoPagoId, setMetodoPagoId] = useState(0);
  const [monto, setMonto] = useState("");
  const [aplicaciones, setAplicaciones] = useState<Record<number, string>>({});
  const [generarCredito, setGenerarCredito] = useState(false);
  const [cargoPage, setCargoPage] = useState(0);
  const [enviando, setEnviando] = useState(false);

  const cargos = useQuery<Page<CargoResponse>>({
    queryKey: queryKeys.cargosPendientes(alumnoId, cargoPage),
    queryFn: () => cargosApi.listarPendientes(alumnoId, cargoPage),
    enabled: alumnoId > 0,
  });

  const seleccionadas = useMemo(() => Object.entries(aplicaciones)
    .filter(([, importe]) => isPositiveMoney(importe))
    .map(([cargoId, importe]) => ({ cargoId: Number(cargoId), importe })), [aplicaciones]);

  const registrar = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!alumnoId || !metodoPagoId || !isPositiveMoney(monto)) {
      toast.error("Completá alumno, método e importe con hasta dos decimales");
      return;
    }
    setEnviando(true);
    try {
      const pago = await pagosApi.registrarPago({
        alumnoId,
        metodoPagoId,
        montoRecibido: monto,
        idempotencyKey: crypto.randomUUID(),
        aplicaciones: seleccionadas,
        generarCredito,
      });
      toast.success(`Pago ${pago.id} registrado`);
      setMonto("");
      setAplicaciones({});
      await queryClient.invalidateQueries({ queryKey: ["cargos", "pendientes", alumnoId] });
    } catch {
      toast.error("El backend rechazó el pago; revisá saldos y aplicaciones");
    } finally {
      setEnviando(false);
    }
  };

  return <main className="form-container">
    <h1>Registrar pago</h1>
    <form onSubmit={registrar}>
      <label>Alumno ID<input type="number" min="1" value={alumnoId || ""} onChange={(e) => { setCargoPage(0); setAlumnoId(Number(e.target.value)); }} /></label>
      <label>Método<select value={metodoPagoId} onChange={(e) => setMetodoPagoId(Number(e.target.value))}>
        <option value={0}>Seleccionar</option>
        {(metodos.data ?? []).filter((method) => method.activo)
          .map((m) => <option key={m.id} value={m.id}>{m.descripcion}</option>)}
      </select></label>
      <label>Monto recibido<input inputMode="decimal" value={monto}
        onChange={(e) => setMonto(e.target.value)} placeholder="0.00" /></label>
      <h2>Cargos</h2>
      {(cargos.data?.content ?? []).map((cargo) => <label key={cargo.id}>
        {cargo.descripcion} — saldo ${cargo.saldo}
        <input aria-label={`Aplicar a ${cargo.descripcion}`} inputMode="decimal"
          value={aplicaciones[cargo.id] ?? ""}
          onChange={(e) => setAplicaciones((current) => ({ ...current, [cargo.id]: e.target.value }))}
          placeholder="0.00" />
      </label>)}
      <div><button type="button" disabled={cargoPage === 0} onClick={() => setCargoPage((value) => value - 1)}>Cargos anteriores</button><span> Página {cargoPage + 1} de {Math.max(cargos.data?.totalPages ?? 1, 1)} </span><button type="button" disabled={!cargos.data || cargoPage + 1 >= cargos.data.totalPages} onClick={() => setCargoPage((value) => value + 1)}>Cargos siguientes</button></div>
      <label><input type="checkbox" checked={generarCredito}
        onChange={(e) => setGenerarCredito(e.target.checked)} /> Generar crédito con el excedente</label>
      <button type="submit" disabled={enviando}>{enviando ? "Registrando…" : "Registrar pago"}</button>
    </form>
  </main>;
}
