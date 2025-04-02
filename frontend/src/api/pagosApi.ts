// pagosApi.ts
import api from "./axiosConfig";
import type {
  PagoRegistroRequest,
  PagoResponse,
  StockResponse,
  CobranzaDTO,
  DisciplinaDetalleResponse,
  PagoParcialRequest,
  DetallePagoResponse,
  AlumnoResponse,
  DetallePagoRegistroRequest,
} from "../types/types";

// Función para descargar el recibo usando una petición GET
const descargarRecibo = async (pagoId: number): Promise<void> => {
  try {
    const response = await api.get(`/pagos/recibo/${pagoId}`, {
      responseType: "blob", // Esto permite recibir el PDF como un blob
    });
    window.open(`/api/pagos/recibo/${pagoId}`, "_blank");

    // Crea una URL a partir del blob recibido
    const url = window.URL.createObjectURL(new Blob([response.data]));
    // Crea un elemento <a> para forzar la descarga
    const link = document.createElement("a");
    link.href = url;
    // Puedes modificar el nombre del archivo si lo deseas
    link.setAttribute("download", "recibo_" + pagoId + ".pdf");
    document.body.appendChild(link);
    link.click();
    // Limpieza: elimina el link y revoca la URL creada
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error("Error al descargar el recibo", error);
  }
};

const registrarPago = async (
  pago: PagoRegistroRequest
): Promise<PagoResponse> => {
  const { data } = await api.post<PagoResponse>("/pagos", pago);
  // Puedes agregar un delay si es necesario
  setTimeout(() => {
    descargarRecibo(data.id);
  }, 500);
  return data;
};

const obtenerPagoPorId = async (id: number): Promise<PagoResponse> => {
  const { data } = await api.get<PagoResponse>(`/pagos/${id}`);
  return data;
};

const obtenerCobranzaPorAlumno = async (
  alumnoId: number
): Promise<CobranzaDTO> => {
  const { data } = await api.get<CobranzaDTO>(
    `/pagos/alumno/${alumnoId}/cobranza`
  );
  return data;
};

const listarDisciplinasBasicas = async (): Promise<
  DisciplinaDetalleResponse[]
> => {
  const { data } = await api.get<DisciplinaDetalleResponse[]>(
    "/disciplinas/listado"
  );
  return data;
};

const listarStocksBasicos = async (): Promise<StockResponse[]> => {
  const { data } = await api.get<StockResponse[]>("/stocks/activos");
  return data;
};

const listarAlumnosBasicos = async (): Promise<AlumnoResponse[]> => {
  const { data } = await api.get<AlumnoResponse[]>("/alumnos/listado");
  return data;
};

/**
 * Lista todos los pagos activos.
 */
const listarPagos = async (): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>("/pagos");
  return data;
};

/**
 * Actualiza un pago.
 */
const actualizarPago = async (
  id: number,
  pago: Partial<PagoRegistroRequest>
): Promise<PagoResponse> => {
  const { data } = await api.put<PagoResponse>(`/pagos/${id}`, pago);
  return data;
};

// Actualiza un pago parcial
const actualizarPagoParcial = async (
  id: number,
  pago: PagoParcialRequest
): Promise<PagoResponse> => {
  const { data } = await api.put<PagoResponse>(`/pagos/${id}/parcial`, pago);
  return data;
};

/**
 * Realiza una baja logica del pago.
 */
const eliminarPago = async (id: number): Promise<void> => {
  await api.delete(`/pagos/${id}`);
};

/**
 * Lista pagos filtrados por inscripcion.
 */
const listarPagosPorInscripcion = async (
  inscripcionId: number
): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>(
    `/pagos/inscripcion/${inscripcionId}`
  );
  return data;
};

/**
 * Lista pagos filtrados por alumno.
 */
const listarPagosPorAlumno = async (
  alumnoId: number
): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>(`/pagos/alumno/${alumnoId}`);
  return data;
};

/**
 * Lista los pagos vencidos.
 */
const listarPagosVencidos = async (): Promise<PagoResponse[]> => {
  const { data } = await api.get<PagoResponse[]>("/pagos/vencidos");
  return data;
};

/**
 * Obtiene el ultimo pago registrado para un alumno.
 * Se asume que el backend tiene un endpoint que responde en:
 * GET /pagos/alumno/{alumnoId}/ultimo
 */
const obtenerUltimoPagoPorAlumno = async (
  alumnoId: number
): Promise<PagoResponse> => {
  const { data } = await api.get<PagoResponse>(
    `/pagos/alumno/${alumnoId}/ultimo`
  );
  return data;
};

const verificarMensualidadOMatricula = async (
  request: DetallePagoRegistroRequest
): Promise<DetallePagoResponse> => {
  const { data } = await api.post<DetallePagoResponse>(
    "/pagos/verificar",
    request
  );
  return data;
};

/**
 * Endpoint para filtrar DetallePagos.
 * Los parámetros son opcionales y se envían en la query string.
 */
const filtrarDetalles = async (params: {
  fechaRegistroDesde?: string;
  fechaRegistroHasta?: string;
  detalleConcepto?: string;
  stock?: string;
  subConcepto?: string;
  disciplina?: string;
  tarifa?: string;
  categoria?: string;
}): Promise<DetallePagoResponse[]> => {
  const { data } = await api.get<DetallePagoResponse[]>("/pagos/filtrar", {
    params,
  });
  return data;
};

const pagosApi = {
  registrarPago,
  obtenerPagoPorId,
  listarPagos,
  actualizarPago,
  descargarRecibo,
  actualizarPagoParcial,
  eliminarPago,
  listarPagosPorInscripcion,
  listarPagosPorAlumno,
  listarPagosVencidos,
  // Nuevos endpoints basicos
  listarDisciplinasBasicas,
  listarStocksBasicos,
  listarAlumnosBasicos,
  obtenerCobranzaPorAlumno, // Agregado aqui
  obtenerUltimoPagoPorAlumno,
  filtrarDetalles, // Agregamos el nuevo endpoint
  verificarMensualidadOMatricula,
};

export default pagosApi;
