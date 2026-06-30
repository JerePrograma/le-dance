import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import alumnosApi from "../../api/alumnosApi";
import cargosApi from "../../api/cargosApi";
import metodosPagoApi from "../../api/metodosPagoApi";
import pagosApi from "../../api/pagosApi";
import type { AlumnoResponse, CargoResponse, MetodoPagoResponse } from "../../types/types";

const DECIMAL = /^(0|[1-9]\d*)(\.\d{1,2})?$/;

export default function PagosFormulario() {
  const [alumnos, setAlumnos] = useState<AlumnoResponse[]>([]);
  const [metodos, setMetodos] = useState<MetodoPagoResponse[]>([]);
  const [cargos, setCargos] = useState<CargoResponse[]>([]);
  const [alumnoId, setAlumnoId] = useState(0);
  const [metodoPagoId, setMetodoPagoId] = useState(0);
  const [monto, setMonto] = useState("");
  const [aplicaciones, setAplicaciones] = useState<Record<number, string>>({});
  const [generarCredito, setGenerarCredito] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    Promise.all([alumnosApi.listar(), metodosPagoApi.listarMetodosPago()])
      .then(([studentData, methodData]) => {
        setAlumnos(studentData);
        setMetodos(methodData.filter((method) => method.activo));
      })
      .catch(() => toast.error("No se pudieron cargar alumnos y métodos de pago"));
  }, []);

  useEffect(() => {
    if (!alumnoId) {
      setCargos([]);
      return;
    }
    cargosApi.listarPendientes(alumnoId).then(setCargos)
      .catch(() => toast.error("No se pudieron cargar los cargos pendientes"));
    setAplicaciones({});
  }, [alumnoId]);

  const seleccionadas = useMemo(() => Object.entries(aplicaciones)
    .filter(([, importe]) => DECIMAL.test(importe) && Number(importe) > 0)
    .map(([cargoId, importe]) => ({ cargoId: Number(cargoId), importe })), [aplicaciones]);

  const registrar = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!alumnoId || !metodoPagoId || !DECIMAL.test(monto) || Number(monto) <= 0) {
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
      setCargos(await cargosApi.listarPendientes(alumnoId));
    } catch {
      toast.error("El backend rechazó el pago; revisá saldos y aplicaciones");
    } finally {
      setEnviando(false);
    }
  };

  return <main className="form-container">
    <h1>Registrar pago</h1>
    <form onSubmit={registrar}>
      <label>Alumno<select value={alumnoId} onChange={(e) => setAlumnoId(Number(e.target.value))}>
        <option value={0}>Seleccionar</option>
        {alumnos.map((a) => <option key={a.id} value={a.id}>{a.apellido}, {a.nombre}</option>)}
      </select></label>
      <label>Método<select value={metodoPagoId} onChange={(e) => setMetodoPagoId(Number(e.target.value))}>
        <option value={0}>Seleccionar</option>
        {metodos.map((m) => <option key={m.id} value={m.id}>{m.descripcion}</option>)}
      </select></label>
      <label>Monto recibido<input inputMode="decimal" value={monto}
        onChange={(e) => setMonto(e.target.value)} placeholder="0.00" /></label>
      <h2>Cargos</h2>
      {cargos.map((cargo) => <label key={cargo.id}>
        {cargo.descripcion} — saldo ${cargo.saldo}
        <input aria-label={`Aplicar a ${cargo.descripcion}`} inputMode="decimal"
          value={aplicaciones[cargo.id] ?? ""}
          onChange={(e) => setAplicaciones((current) => ({ ...current, [cargo.id]: e.target.value }))}
          placeholder="0.00" />
      </label>)}
      <label><input type="checkbox" checked={generarCredito}
        onChange={(e) => setGenerarCredito(e.target.checked)} /> Generar crédito con el excedente</label>
      <button type="submit" disabled={enviando}>{enviando ? "Registrando…" : "Registrar pago"}</button>
    </form>
  </main>;
}
