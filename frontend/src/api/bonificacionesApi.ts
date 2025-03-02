import api from "./axiosConfig";
import type {
  BonificacionRegistroRequest,
  BonificacionModificacionRequest,
  BonificacionResponse,
} from "../types/types";

const bonificacionesApi = {
  crearBonificacion: async (
    bonificacion: BonificacionRegistroRequest
  ): Promise<BonificacionResponse> => {
    const response = await api.post("/bonificaciones", bonificacion);
    return response.data;
  },

  listarBonificaciones: async (): Promise<BonificacionResponse[]> => {
    const response = await api.get("/bonificaciones");
    return response.data;
  },

  obtenerBonificacionPorId: async (
    id: number
  ): Promise<BonificacionResponse> => {
    const response = await api.get(`/bonificaciones/${id}`);
    return response.data;
  },

  actualizarBonificacion: async (
    id: number,
    bonificacion: BonificacionModificacionRequest
  ): Promise<BonificacionResponse> => {
    const response = await api.put(`/bonificaciones/${id}`, bonificacion);
    return response.data;
  },

  eliminarBonificacion: async (id: number): Promise<void> => {
    await api.delete(`/bonificaciones/${id}`);
  },
};

export default bonificacionesApi;
