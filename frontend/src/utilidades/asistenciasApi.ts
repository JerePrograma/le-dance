import api from "./axiosConfig";
import { AsistenciaRequest, AsistenciaResponse } from "../types/types";

const asistenciasApi = {
  listarAsistencias: async (): Promise<AsistenciaResponse[]> => {
    const response = await api.get("/api/asistencias"); // ✅ Cambiado para devolver solo los datos necesarios
    return response.data;
  },

  registrarAsistencia: async (
    asistencia: AsistenciaRequest
  ): Promise<AsistenciaResponse> => {
    const response = await api.post("/api/asistencias", asistencia);
    return response.data;
  },

  obtenerAsistenciasPorDisciplina: async (
    disciplinaId: number
  ): Promise<AsistenciaResponse[]> => {
    const response = await api.get(
      `/api/asistencias/disciplina/${disciplinaId}`
    );
    return response.data;
  },

  obtenerAsistenciasPorAsistencia: async (
    asistenciaId: number
  ): Promise<AsistenciaResponse[]> => {
    const response = await api.get(
      `/api/asistencias/asistencia/${asistenciaId}`
    );
    return response.data;
  },

  obtenerAsistenciasPorFechaYDisciplina: async (
    fecha: string,
    disciplinaId: number
  ): Promise<AsistenciaResponse[]> => {
    const response = await api.get(`/api/asistencias/fecha`, {
      params: { fecha, disciplinaId },
    });
    return response.data;
  },

  eliminarAsistencia: async (id: number): Promise<string> => {
    const response = await api.put(`/api/asistencias/${id}`, { activo: false });
    return response.data;
  },
};

export default asistenciasApi;
