// /src/utilidades/alumnosApi.ts
import api from "./axiosConfig";
import {
  AlumnoListadoResponse,
  AlumnoRequest,
  AlumnoResponse,
} from "../types/types";

const alumnosApi = {
  listarAlumnos: async (): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get("/api/alumnos/listado"); // ✅ Cambiado para devolver solo los datos necesarios
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
    const response = await api.delete(`/api/alumnos/${id}`);
    return response.data;
  },

  buscarPorNombre: async (nombre: string): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(
      `/api/alumnos/buscar?nombre=${encodeURIComponent(nombre)}`
    );
    return response.data;
  },
};

export default alumnosApi;
