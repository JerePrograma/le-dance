import api from "./axiosConfig";
import { BonificacionResponse, BonificacionRequest } from "../types/types";

const bonificacionesApi = {
  listarBonificaciones: async (): Promise<BonificacionResponse[]> => {
    const response = await api.get("/api/bonificaciones");
    return response.data;
  },
  obtenerBonificacionPorId: async (
    id: number
  ): Promise<BonificacionResponse> => {
    const response = await api.get(`/api/bonificaciones/${id}`);
    return response.data;
  },
  crearBonificacion: async (
    bonificacion: BonificacionRequest
  ): Promise<BonificacionResponse> => {
    const response = await api.post("/api/bonificaciones", bonificacion);
    return response.data;
  },
  actualizarBonificacion: async (
    id: number,
    bonificacion: BonificacionRequest
  ): Promise<BonificacionResponse> => {
    const response = await api.put(`/api/bonificaciones/${id}`, bonificacion);
    return response.data;
  },
  eliminarBonificacion: async (id: number): Promise<string> => {
    const response = await api.put(`/api/bonificaciones/${id}`, {
      activo: false,
    });
    return response.data;
  },
};

export default bonificacionesApi;
