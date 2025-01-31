import api from "./axiosConfig";
import { AsistenciaRequest, AsistenciaResponse } from "../types/types";

const asistenciasApi = {
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

  obtenerAsistenciasPorAlumno: async (
    alumnoId: number
  ): Promise<AsistenciaResponse[]> => {
    const response = await api.get(`/api/asistencias/alumno/${alumnoId}`);
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
