import api from "./axiosConfig";
import type {
  ProfesorRegistroRequest,
  ProfesorModificacionRequest,
  ProfesorDetalleResponse,
  ProfesorListadoResponse,
  DisciplinaListadoResponse,
} from "../types/types";

const profesoresApi = {
  registrarProfesor: async (
    profesor: ProfesorRegistroRequest
  ): Promise<ProfesorDetalleResponse> => {
    console.log("Datos enviados al registrar:", profesor);
    const response = await api.post("/profesores", profesor);
    return response.data;
  },

  obtenerProfesorPorId: async (
    id: number
  ): Promise<ProfesorDetalleResponse> => {
    const response = await api.get(`/profesores/${id}`);
    return response.data;
  },

  listarProfesores: async (): Promise<ProfesorListadoResponse[]> => {
    const response = await api.get("/profesores");
    return response.data;
  },

  listarProfesoresActivos: async (): Promise<ProfesorListadoResponse[]> => {
    const response = await api.get("/profesores/activos");
    return response.data;
  },

  buscarPorNombre: async (nombre: string): Promise<ProfesorListadoResponse[]> => {
    const response = await api.get(
      `/profesores/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return response.data;
  },

  actualizarProfesor: async (
    id: number,
    profesor: ProfesorModificacionRequest
  ): Promise<ProfesorDetalleResponse> => {
    console.log("Datos enviados al actualizar:", profesor);
    const response = await api.put(`/profesores/${id}`, profesor);
    return response.data;
  },

  eliminarProfesor: async (id: number): Promise<void> => {
    await api.delete(`/profesores/${id}`);
  },

  obtenerDisciplinasDeProfesor: async (
    profesorId: number
  ): Promise<DisciplinaListadoResponse[]> => {
    const response = await api.get(`/profesores/${profesorId}/disciplinas`);
    return response.data;
  },
};

export default profesoresApi;
