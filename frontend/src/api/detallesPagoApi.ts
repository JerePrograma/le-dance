// detallesPagoApi.ts
import api from "./axiosConfig";
import type {
  DetallePagoResponse,
  DetallePagoRegistroRequest,
} from "../types/types";

// Crea un nuevo DetallePagoRegistroRequest
const crearDetallePago = async (
  detalle: DetallePagoRegistroRequest
): Promise<DetallePagoResponse> => {
  const { data } = await api.post<DetallePagoResponse>(
    "/detalle-pago",
    detalle
  );
  return data;
};

// Obtiene un DetallePagoRegistroRequest por su ID
const obtenerDetallePagoPorId = async (
  id: number
): Promise<DetallePagoResponse> => {
  const { data } = await api.get<DetallePagoResponse>(`/detalle-pago/${id}`);
  return data;
};

// Actualiza un DetallePagoRegistroRequest existente
const actualizarDetallePago = async (
  id: number,
  detalle: DetallePagoRegistroRequest
): Promise<DetallePagoResponse> => {
  const { data } = await api.put<DetallePagoResponse>(
    `/detalle-pago/${id}`,
    detalle
  );
  return data;
};

// Elimina un DetallePagoRegistroRequest por su ID
const eliminarDetallePago = async (id: number): Promise<void> => {
  await api.delete(`/detalle-pago/${id}`);
};

const anularDetallePago = async (id: number): Promise<DetallePagoResponse> => {
  const response = await api.put(`/detalle-pago/anular/${id}`);
  return response.data; // Asegúrate de que response.data tenga la estructura de DetallePagoResponse
};

// Lista todos los DetallePagos
const listarDetallesPagos = async (): Promise<DetallePagoResponse[]> => {
  const { data } = await api.get<DetallePagoResponse[]>("/detalle-pago");
  return data;
};

const detallesPagoApi = {
  crearDetallePago,
  obtenerDetallePagoPorId,
  actualizarDetallePago,
  eliminarDetallePago,
  listarDetallesPagos,
  anularDetallePago,
};

export default detallesPagoApi;
