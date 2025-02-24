// src/api/metodosPagoApi.ts
import api from "./axiosConfig";
import type {
  MetodoPagoRegistroRequest,
  MetodoPagoModificacionRequest,
  MetodoPagoResponse,
} from "../types/types";

const registrarMetodoPago = async (
  request: MetodoPagoRegistroRequest
): Promise<MetodoPagoResponse> => {
  const { data } = await api.post<MetodoPagoResponse>(
    "/api/metodos-pago",
    request
  );
  return data;
};

const obtenerMetodoPagoPorId = async (
  id: number
): Promise<MetodoPagoResponse> => {
  const { data } = await api.get<MetodoPagoResponse>(`/api/metodos-pago/${id}`);
  return data;
};

const listarMetodosPago = async (): Promise<MetodoPagoResponse[]> => {
  const { data } = await api.get<MetodoPagoResponse[]>("/api/metodos-pago");
  return data;
};

const actualizarMetodoPago = async (
  id: number,
  request: MetodoPagoModificacionRequest
): Promise<MetodoPagoResponse> => {
  const { data } = await api.put<MetodoPagoResponse>(
    `/api/metodos-pago/${id}`,
    request
  );
  return data;
};

const eliminarMetodoPago = async (id: number): Promise<void> => {
  await api.delete(`/api/metodos-pago/${id}`);
};

const metodosPagoApi = {
  registrarMetodoPago,
  obtenerMetodoPagoPorId,
  listarMetodosPago,
  actualizarMetodoPago,
  eliminarMetodoPago,
};

export default metodosPagoApi;
