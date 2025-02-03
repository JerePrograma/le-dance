import api from "./axiosConfig";
import {
  AsistenciaRequest,
  AsistenciaResponse,
  AlumnoListadoResponse,
  DisciplinaResponse,
  ProfesorListadoResponse,
} from "../types/types";

const asistenciasApi = {
  /** 🔹 Obtener todas las asistencias */
  listarAsistencias: async (): Promise<AsistenciaResponse[]> => {
    try {
      const response = await api.get("/api/asistencias");

      return response.data.map((asistencia: AsistenciaResponse) => ({
        ...asistencia,
        alumno: {
          ...asistencia.alumno,
          activo: asistencia.alumno.activo ?? false, // ✅ Asignar `false` si es `undefined`
        },
      }));
    } catch (error) {
      console.error("Error al obtener asistencias:", error);
      throw error;
    }
  },

  /** 🔹 Obtener una asistencia por ID */
  obtenerAsistenciaPorId: async (id: number): Promise<AsistenciaResponse> => {
    try {
      const response = await api.get(`/api/asistencias/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error al obtener la asistencia con ID ${id}:`, error);
      throw error;
    }
  },

  /** 🔹 Registrar una nueva asistencia */
  registrarAsistencia: async (
    asistencia: AsistenciaRequest
  ): Promise<AsistenciaResponse> => {
    try {
      const response = await api.post("/api/asistencias", asistencia);
      return response.data;
    } catch (error) {
      console.error("Error al registrar asistencia:", error);
      throw error;
    }
  },

  /** 🔹 Actualizar una asistencia existente */
  actualizarAsistencia: async (
    id: number,
    asistencia: AsistenciaRequest
  ): Promise<AsistenciaResponse> => {
    try {
      const response = await api.put(`/api/asistencias/${id}`, asistencia);
      return response.data;
    } catch (error) {
      console.error(`Error al actualizar asistencia con ID ${id}:`, error);
      throw error;
    }
  },

  /** 🔹 Eliminar (desactivar) una asistencia */
  eliminarAsistencia: async (id: number): Promise<void> => {
    try {
      await api.put(`/api/asistencias/${id}`, { activo: false });
    } catch (error) {
      console.error(`Error al eliminar asistencia con ID ${id}:`, error);
      throw error;
    }
  },

  /** 🔹 Obtener alumnos de una disciplina */
  obtenerAlumnosDeDisciplina: async (
    disciplinaId: number
  ): Promise<AlumnoListadoResponse[]> => {
    try {
      const response = await api.get(
        `/api/disciplinas/${disciplinaId}/alumnos`
      );
      return response.data;
    } catch (error) {
      console.error("Error al obtener alumnos de la disciplina:", error);
      throw error;
    }
  },

  /** 🔹 Obtener profesores de una disciplina */
  obtenerProfesoresDeDisciplina: async (
    disciplinaId: number
  ): Promise<ProfesorListadoResponse[]> => {
    try {
      const response = await api.get(
        `/api/disciplinas/${disciplinaId}/profesores`
      );
      return response.data;
    } catch (error) {
      console.error("Error al obtener profesores de la disciplina:", error);
      throw error;
    }
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

  /** 🔹 Obtener disciplinas de un profesor */
  obtenerDisciplinasDeProfesor: async (
    profesorId: number
  ): Promise<DisciplinaResponse[]> => {
    try {
      const response = await api.get(
        `/api/profesores/${profesorId}/disciplinas`
      );
      return response.data;
    } catch (error) {
      console.error("Error al obtener disciplinas del profesor:", error);
      throw error;
    }
  },
};

export default asistenciasApi;
