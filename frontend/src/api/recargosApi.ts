import api from "./axiosConfig";
import type {
  RecargoRegistroRequest,
  RecargoResponse,
  RecargoModificacionRequest,
} from "../types/types";

const recargosApi = {
  crearRecargo: async (
    recargo: RecargoRegistroRequest
  ): Promise<RecargoResponse> => {
    const response = await api.post("/recargos", recargo);
    return response.data;
  },

  listarRecargos: async (): Promise<RecargoResponse[]> => {
    const response = await api.get("/recargos");
    return response.data;
  },

  obtenerRecargoPorId: async (id: number): Promise<RecargoResponse> => {
    const response = await api.get(`/recargos/${id}`);
    return response.data;
  },

  actualizarRecargo: async (
    id: number,
    recargo: RecargoModificacionRequest
  ): Promise<RecargoResponse> => {
    const response = await api.put(`/recargos/${id}`, recargo);
    return response.data;
  },

  eliminarRecargo: async (id: number): Promise<void> => {
    await api.delete(`/recargos/${id}`);
  },
};

export default recargosApi;
