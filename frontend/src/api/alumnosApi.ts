import api from "./axiosConfig";
import type {
  AlumnoListadoResponse,
  AlumnoRegistroRequest,
  AlumnoModificacionRequest,
  AlumnoDetalleResponse,
  DisciplinaListadoResponse,
} from "../types/types";

const alumnosApi = {
  listar: async (): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get("/api/alumnos");
    return response.data;
  },

  obtenerPorId: async (id: number): Promise<AlumnoDetalleResponse> => {
    const response = await api.get(`/api/alumnos/${id}`);
    return response.data;
  },

  registrar: async (
    alumno: AlumnoRegistroRequest
  ): Promise<AlumnoDetalleResponse> => {
    const response = await api.post("/api/alumnos", alumno);
    return response.data;
  },

  actualizar: async (
    id: number,
    alumno: AlumnoModificacionRequest
  ): Promise<AlumnoDetalleResponse> => {
    const response = await api.put(`/api/alumnos/${id}`, alumno);
    return response.data;
  },

  darBaja: async (id: number): Promise<void> => {
    await api.delete(`/api/alumnos/${id}`);
  },

  obtenerListadoSimplificado: async (): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get("/api/alumnos/listado");
    return response.data;
  },

  buscarPorNombre: async (nombre: string): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(
      `/api/alumnos/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return response.data;
  },

  obtenerDisciplinas: async (
    alumnoId: number
  ): Promise<DisciplinaListadoResponse[]> => {
    const response = await api.get(`/api/alumnos/${alumnoId}/disciplinas`);
    return response.data;
  },
};

export default alumnosApi;
