import api from "./axiosConfig";
import type {
  InscripcionRegistroRequest,
  InscripcionModificacionRequest,
  InscripcionResponse,
} from "../types/types";

const inscripcionesApi = {
  crear: async (
    request: InscripcionRegistroRequest
  ): Promise<InscripcionResponse> => {
    const response = await api.post("/api/inscripciones", request);
    return response.data;
  },

  listar: async (alumnoId?: number): Promise<InscripcionResponse[]> => {
    const url = alumnoId
      ? `/api/inscripciones?alumnoId=${alumnoId}`
      : "/api/inscripciones";
    const response = await api.get(url);
    return response.data;
  },

  obtenerPorId: async (id: number): Promise<InscripcionResponse> => {
    const response = await api.get(`/api/inscripciones/${id}`);
    return response.data;
  },

  listarPorDisciplina: async (
    disciplinaId: number
  ): Promise<InscripcionResponse[]> => {
    const response = await api.get(
      `/api/inscripciones/disciplina/${disciplinaId}`
    );
    return response.data;
  },

  actualizar: async (
    id: number,
    request: InscripcionModificacionRequest
  ): Promise<InscripcionResponse> => {
    const response = await api.put(`/api/inscripciones/${id}`, request);
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`/api/inscripciones/${id}`);
  },
};

export default inscripcionesApi;
