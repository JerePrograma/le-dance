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
    const response = await api.get("/alumnos");
    return response.data;
  },

  obtenerPorId: async (id: number): Promise<AlumnoDetalleResponse> => {
    const response = await api.get(`/alumnos/${id}`);
    return response.data;
  },

  registrar: async (
    alumno: AlumnoRegistroRequest
  ): Promise<AlumnoDetalleResponse> => {
    const response = await api.post("/alumnos", alumno);
    return response.data;
  },

  actualizar: async (
    id: number,
    alumno: AlumnoModificacionRequest
  ): Promise<AlumnoDetalleResponse> => {
    const response = await api.put(`/alumnos/${id}`, alumno);
    return response.data;
  },

  darBaja: async (id: number): Promise<void> => {
    await api.delete(`/alumnos/dar-baja/${id}`);
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`/alumnos/${id}`);
  },

  obtenerListadoSimplificado: async (): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get("/alumnos/listado");
    return response.data;
  },

  buscarPorNombre: async (nombre: string): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(
      `/alumnos/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return response.data;
  },

  obtenerDisciplinas: async (
    alumnoId: number
  ): Promise<DisciplinaListadoResponse[]> => {
    const response = await api.get(`/alumnos/${alumnoId}/disciplinas`);
    return response.data;
  },

};

export default alumnosApi;
