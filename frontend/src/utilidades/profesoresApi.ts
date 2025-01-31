import api from "./axiosConfig";
import { ProfesorRequest, ProfesorResponse } from "../types/types";

const profesoresApi = {
  listarProfesores: async (): Promise<ProfesorResponse[]> => {
    const response = await api.get("/api/profesores");
    return response.data;
  },

  obtenerProfesorPorId: async (id: number): Promise<ProfesorResponse> => {
    const response = await api.get(`/api/profesores/${id}`);
    return response.data;
  },

  registrarProfesor: async (
    profesor: ProfesorRequest
  ): Promise<ProfesorResponse> => {
    const response = await api.post("/api/profesores", profesor);
    return response.data;
  },

  asignarUsuario: async (
    profesorId: number,
    usuarioId: number
  ): Promise<string> => {
    const response = await api.patch(
      `/api/profesores/${profesorId}/asignar-usuario?usuarioId=${usuarioId}`
    );
    return response.data;
  },

  asignarDisciplina: async (
    profesorId: number,
    disciplinaId: number
  ): Promise<string> => {
    const response = await api.patch(
      `/api/profesores/${profesorId}/asignar-disciplina/${disciplinaId}`
    );
    return response.data;
  },

  eliminarProfesor: async (id: number): Promise<string> => {
    const response = await api.put(`/api/profesores/${id}`, { activo: false });
    return response.data;
  },
};

export default profesoresApi;
