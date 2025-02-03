import api from "./axiosConfig";
import {
  AsistenciaRequest,
  AsistenciaResponse,
  AlumnoListadoResponse,
  DisciplinaResponse,
  ProfesorResponse,
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

  /** 🔹 Obtener asistencias filtradas por disciplina */
  obtenerAsistenciasPorDisciplina: async (
    disciplinaId: number
  ): Promise<AsistenciaResponse[]> => {
    try {
      const response = await api.get(
        `/api/asistencias/disciplina/${disciplinaId}`
      );
      return response.data;
    } catch (error) {
      console.error("Error al obtener asistencias por disciplina:", error);
      throw error;
    }
  },

  /** 🔹 Obtener asistencias filtradas por alumno */
  obtenerAsistenciasPorAlumno: async (
    alumnoId: number
  ): Promise<AsistenciaResponse[]> => {
    try {
      const response = await api.get(`/api/asistencias/alumno/${alumnoId}`);
      return response.data;
    } catch (error) {
      console.error("Error al obtener asistencias por alumno:", error);
      throw error;
    }
  },

  /** 🔹 Obtener asistencias por fecha y disciplina */
  obtenerAsistenciasPorFechaYDisciplina: async (
    fecha: string,
    disciplinaId: number
  ): Promise<AsistenciaResponse[]> => {
    try {
      const response = await api.get(`/api/asistencias/fecha`, {
        params: { fecha, disciplinaId },
      });
      return response.data;
    } catch (error) {
      console.error(
        "Error al obtener asistencias por fecha y disciplina:",
        error
      );
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

  /** 🔹 Obtener la lista de profesores */
  obtenerProfesores: async (): Promise<ProfesorResponse[]> => {
    try {
      const response = await api.get("/api/profesores");
      return response.data;
    } catch (error) {
      console.error("Error al obtener profesores:", error);
      throw error;
    }
  },

  /** 🔹 Obtener la lista de alumnos (formato simplificado) */
  obtenerAlumnosListado: async (): Promise<AlumnoListadoResponse[]> => {
    try {
      const response = await api.get("/api/alumnos/listado");
      return response.data;
    } catch (error) {
      console.error("Error al obtener alumnos:", error);
      throw error;
    }
  },

  /** 🔹 Obtener la lista de disciplinas */
  obtenerDisciplinas: async (): Promise<DisciplinaResponse[]> => {
    try {
      const response = await api.get("/api/disciplinas");
      return response.data;
    } catch (error) {
      console.error("Error al obtener disciplinas:", error);
      throw error;
    }
  },

  /** 🔹 Obtener alumnos filtrados por fecha y disciplina */
  obtenerAlumnosPorFechaYDisciplina: async (
    fecha: string,
    disciplinaId: number
  ): Promise<AlumnoListadoResponse[]> => {
    try {
      const response = await api.get("/api/alumnos/por-fecha-y-disciplina", {
        params: { fecha, disciplinaId },
      });
      return response.data;
    } catch (error) {
      console.error("Error al obtener alumnos por fecha y disciplina:", error);
      throw error;
    }
  },
};

export default asistenciasApi;
