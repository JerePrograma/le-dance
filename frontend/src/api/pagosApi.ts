import api from "./axiosConfig";
import type { Page, PagoAnulacionRequest, PagoRegistroRequest, PagoResponse, PagoResumenResponse } from "../types/types";

const registrarPago = async (request: PagoRegistroRequest): Promise<PagoResponse> =>
  (await api.post<PagoResponse>("/pagos", request)).data;

const obtenerPagoPorId = async (id: number): Promise<PagoResponse> =>
  (await api.get<PagoResponse>(`/pagos/${id}`)).data;

const listarPagosPorAlumno = async (alumnoId: number, page = 0, size = 50): Promise<Page<PagoResumenResponse>> =>
  (await api.get<Page<PagoResumenResponse>>(`/pagos/alumno/${alumnoId}`, { params: { page, size } })).data;

const anularPago = async (id: number, request: PagoAnulacionRequest): Promise<PagoResponse> =>
  (await api.post<PagoResponse>(`/pagos/${id}/anulacion`, request)).data;

const descargarRecibo = async (pagoId: number): Promise<void> => {
  const { data } = await api.get<Blob>(`/pagos/recibo/${pagoId}`, { responseType: "blob" });
  const url = URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = url;
  link.download = `recibo_${pagoId}.pdf`;
  link.click();
  URL.revokeObjectURL(url);
};

export default { registrarPago, obtenerPagoPorId, listarPagosPorAlumno, anularPago, descargarRecibo };
