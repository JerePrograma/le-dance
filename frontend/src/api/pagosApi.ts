// pagosApi.ts
import api from "./axiosConfig";
import type {
  PagoRegistroRequest,
  PagoModificacionRequest,
  PagoResponse,
  DisciplinaListadoResponse,
  StockResponse,
  AlumnoListadoResponse,
  CobranzaDTO,
} from "../types/types";

/**
 * Registra un nuevo pago.
 */
const registrarPago = async (
  pago: PagoRegistroRequest
): Promise<PagoResponse> => {
  const { data } = await api.post<PagoResponse>("/api/pagos", pago);
  return data;
};

/**
 * Obtiene un pago por ID.
 */
const obtenerPagoPorId = async (id: number): Promise<PagoResponse> => {
  const { data } = await api.get<PagoResponse>(`/api/pagos/${id}`);
  return data;
};

/**
 * Lista todos los pagos activos.
 */
const listarPagos = async (): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>("/api/pagos");
  return data;
};

/**
 * Actualiza un pago.
 */
const actualizarPago = async (
  id: number,
  pago: Partial<PagoModificacionRequest>
): Promise<PagoResponse> => {
  const { data } = await api.put<PagoResponse>(`/api/pagos/${id}`, pago);
  return data;
};

/**
 * Realiza una baja lógica del pago.
 */
const eliminarPago = async (id: number): Promise<void> => {
  await api.delete(`/api/pagos/${id}`);
};

/**
 * Lista pagos filtrados por inscripción.
 */
const listarPagosPorInscripcion = async (
  inscripcionId: number
): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>(
    `/api/pagos/inscripcion/${inscripcionId}`
  );
  return data;
};

/**
 * Lista pagos filtrados por alumno.
 */
const listarPagosPorAlumno = async (
  alumnoId: number
): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>(
    `/api/pagos/alumno/${alumnoId}`
  );
  return data;
};

/**
 * Lista los pagos vencidos.
 */
const listarPagosVencidos = async (): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>("/api/pagos/vencidos");
  return data;
};

/**
 * NUEVOS ENDPOINTS BASICOS:
 */

/**
 * Lista de forma básica las disciplinas.
 */
const listarDisciplinasBasicas = async (): Promise<
  DisciplinaListadoResponse[]
> => {
  const { data } = await api.get<DisciplinaListadoResponse[]>(
    "/api/disciplinas/listado"
  );
  return data;
};

/**
 * Lista de forma básica los stocks.
 */
const listarStocksBasicos = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/api/stocks/activos");
  return data;
};

/**
 * Lista de forma básica los alumnos.
 */
const listarAlumnosBasicos = async (): Promise<AlumnoListadoResponse[]> => {
  const { data } = await api.get<AlumnoListadoResponse[]>(
    "/api/alumnos/listado"
  );
  return data;
};

const obtenerCobranzaPorAlumno = async (
  alumnoId: number
): Promise<CobranzaDTO> => {
  const { data } = await api.get<CobranzaDTO>(
    `/api/pagos/alumno/${alumnoId}/cobranza`
  );
  return data;
};

const pagosApi = {
  registrarPago,
  obtenerPagoPorId,
  listarPagos,
  actualizarPago,
  eliminarPago,
  listarPagosPorInscripcion,
  listarPagosPorAlumno,
  listarPagosVencidos,
  // Nuevos endpoints básicos
  listarDisciplinasBasicas,
  listarStocksBasicos,
  listarAlumnosBasicos,
  obtenerCobranzaPorAlumno, // Agregado aquí
};

export default pagosApi;
