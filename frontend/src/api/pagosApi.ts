// pagosApi.ts
import api from "./axiosConfig";
import type {
  PagoRegistroRequest,
  PagoModificacionRequest,
  PagoResponse,
} from "../types/types";
// Si quieres utilizar el tipo refinado para registro, podrías importarlo así:
// import type { NewPagoRegistroRequest } from "../types/pagoTypes";

/**
 * Registra un nuevo pago.
 * Se podría usar un tipo como NewPagoRegistroRequest si queremos que el front solo envíe los datos necesarios.
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
 * Actualiza un pago. Se utiliza Partial<PagoModificacionRequest> para permitir actualizar solo los campos necesarios.
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
 * Lista pagos filtrados por la inscripción.
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
 * Lista pagos filtrados por el alumno.
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

const pagosApi = {
  registrarPago,
  obtenerPagoPorId,
  listarPagos,
  actualizarPago,
  eliminarPago,
  listarPagosPorInscripcion,
  listarPagosPorAlumno,
  listarPagosVencidos,
};

export default pagosApi;
