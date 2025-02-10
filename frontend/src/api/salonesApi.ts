import api from "./axiosConfig";
import type {
  SalonRegistroRequest,
  SalonModificacionRequest,
  SalonResponse,
  Page,
} from "../types/types";

const salonesApi = {
  registrarSalon: async (
    salon: SalonRegistroRequest
  ): Promise<SalonResponse> => {
    const response = await api.post("/api/salones", salon);
    return response.data;
  },

  listarSalones: async (page = 0, size = 10): Promise<Page<SalonResponse>> => {
    const response = await api.get(`/api/salones?page=${page}&size=${size}`);
    return response.data;
  },

  obtenerSalonPorId: async (id: number): Promise<SalonResponse> => {
    const response = await api.get(`/api/salones/${id}`);
    return response.data;
  },

  actualizarSalon: async (
    id: number,
    salon: SalonModificacionRequest
  ): Promise<SalonResponse> => {
    const response = await api.put(`/api/salones/${id}`, salon);
    return response.data;
  },

  eliminarSalon: async (id: number): Promise<void> => {
    await api.delete(`/api/salones/${id}`);
  },
};

export default salonesApi;
