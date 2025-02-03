import api from "./axiosConfig";
import {
  ProfesorRequest,
  ProfesorResponse,
  DisciplinaResponse,
} from "../types/types";

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

  eliminarProfesor: async (id: number): Promise<string> => {
    const response = await api.put(`/api/profesores/${id}`, { activo: false });
    return response.data;
  },

  /** 🔹 Obtener disciplinas de un profesor */
  obtenerDisciplinasDeProfesor: async (
    profesorId: number
  ): Promise<DisciplinaResponse[]> => {
    const response = await api.get(`/api/profesores/${profesorId}/disciplinas`);
    return response.data;
  },
};

export default profesoresApi;
