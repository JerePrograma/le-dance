// /src/utilidades/alumnosApi.ts
import api from "./axiosConfig";
import {
  AlumnoListadoResponse,
  AlumnoRequest,
  AlumnoResponse,
  DisciplinaResponse,
} from "../types/types";

const alumnosApi = {
  listarAlumnos: async (): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get("/api/alumnos/listado"); // ✅ Devuelve solo los datos necesarios
    return response.data;
  },

  obtenerAlumnoPorId: async (id: number): Promise<AlumnoResponse> => {
    const response = await api.get(`/api/alumnos/${id}`);
    return response.data;
  },

  registrarAlumno: async (alumno: AlumnoRequest): Promise<AlumnoResponse> => {
    const response = await api.post("/api/alumnos", alumno);
    return response.data;
  },

  actualizarAlumno: async (
    id: number,
    alumno: AlumnoRequest
  ): Promise<AlumnoResponse> => {
    const response = await api.put(`/api/alumnos/${id}`, alumno);
    return response.data;
  },

  eliminarAlumno: async (id: number): Promise<string> => {
    const response = await api.put(`/api/alumnos/${id}`, { activo: false });
    return response.data;
  },

  buscarPorNombre: async (nombre: string): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(
      `/api/alumnos/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return response.data;
  },

  /** 🔹 Obtener disciplinas de un alumno */
  obtenerDisciplinasDeAlumno: async (
    alumnoId: number
  ): Promise<DisciplinaResponse[]> => {
    try {
      const response = await api.get(`/api/alumnos/${alumnoId}/disciplinas`);
      return response.data;
    } catch (error) {
      console.error("Error al obtener disciplinas del alumno:", error);
      throw error;
    }
  },
};

export default alumnosApi;
