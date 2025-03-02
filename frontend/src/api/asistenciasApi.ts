import api from "./axiosConfig";
import { toast } from "react-toastify";
import type {
  AsistenciaDiariaRegistroRequest,
  AsistenciaDiariaResponse,
  AsistenciaMensualModificacionRequest,
  AsistenciaMensualDetalleResponse,
  AsistenciaMensualListadoResponse,
  PageResponse,
  AsistenciaMensualRegistroRequest,
  DisciplinaListadoResponse,
} from "../types/types";

// Cache implementation
const asistenciaCache = new Map<number, AsistenciaMensualDetalleResponse>();

const asistenciasApi = {
  // Método para obtener el detalle de la asistencia mensual (solo obtener, sin crear)
  // En asistenciasApi.ts

  obtenerAsistenciaMensualDetallePorParametros: async (
    disciplinaId: number,
    mes: number,
    anio: number
  ): Promise<AsistenciaMensualDetalleResponse | null> => {
    try {
      const response = await api.get(
        "/asistencias-mensuales/por-disciplina/detalle",
        {
          params: { disciplinaId, mes, anio },
        }
      );
      return response.data;
    } catch (error: any) {
      console.error(
        "Error al obtener asistencia mensual por parámetros:",
        error
      );
      toast.error(
        "Error al obtener la asistencia mensual. Intente nuevamente."
      );
      return null;
    }
  },

  // Método para crear la asistencia mensual (solo crear, sin buscar)
  crearAsistenciaMensualPorDisciplina: async (
    disciplinaId: number,
    mes: number,
    anio: number
  ): Promise<AsistenciaMensualDetalleResponse> => {
    try {
      const response = await api.post(
        "/asistencias-mensuales/por-disciplina/crear",
        {
          disciplinaId,
          mes,
          anio,
        }
      );
      return response.data;
    } catch (error: any) {
      console.error("Error al crear asistencia mensual por disciplina:", error);
      toast.error("Error al crear la asistencia mensual. Intente nuevamente.");
      throw error;
    }
  },

  // Los métodos existentes permanecen sin cambios (listar, actualizar, etc.)
  listarAsistenciasMensuales: async (
    profesorId?: number,
    disciplinaId?: number,
    mes?: number,
    anio?: number
  ): Promise<AsistenciaMensualListadoResponse[]> => {
    try {
      const response = await api.get("/asistencias-mensuales", {
        params: { profesorId, disciplinaId, mes, anio },
      });
      return response.data;
    } catch (error: any) {
      console.error(
        "Error al obtener listado de asistencias mensuales:",
        error
      );
      if (error.response?.status === 404) {
        toast.warn(
          "No se encontraron asistencias para los criterios seleccionados."
        );
        return [];
      }
      toast.error(
        "Error al obtener listado de asistencias. Intente nuevamente."
      );
      throw error;
    }
  },

  obtenerAsistenciasDiarias: async (
    asistenciaMensualId: number
  ): Promise<AsistenciaDiariaResponse[]> => {
    try {
      const response = await api.get(
        `/asistencias-diarias/por-asistencia-mensual/${asistenciaMensualId}`
      );
      return response.data;
    } catch (error: any) {
      console.error("Error al obtener asistencias diarias:", error);
      toast.error("Error al obtener asistencias. Intente nuevamente.");
      throw error;
    }
  },

  obtenerAsistenciaMensualDetalle: async (
    id: number
  ): Promise<AsistenciaMensualDetalleResponse | null> => {
    try {
      const response = await api.get(`/asistencias-mensuales/${id}/detalle`);
      return response.data;
    } catch (error: any) {
      console.error("Error al obtener detalle de asistencia mensual:", error);
      if (error.response?.status === 404) {
        toast.warn("No se encontró la asistencia mensual.");
      } else {
        toast.error(
          "Error al obtener la asistencia mensual. Intente nuevamente."
        );
      }
      return null;
    }
  },

  actualizarAsistenciaMensual: async (
    id: number,
    asistencia: AsistenciaMensualModificacionRequest
  ): Promise<AsistenciaMensualDetalleResponse> => {
    try {
      const response = await api.put(
        `/asistencias-mensuales/${id}`,
        asistencia
      );
      // Update cache
      asistenciaCache.set(id, response.data);
      return response.data;
    } catch (error: any) {
      console.error("Error al actualizar asistencia mensual:", error);
      toast.error("Error al actualizar asistencia. Intente nuevamente.");
      throw error;
    }
  },

  obtenerAsistenciasPorDisciplinaYFecha: async (
    disciplinaId: number,
    fecha: string,
    page = 0,
    size = 10
  ): Promise<PageResponse<AsistenciaDiariaResponse>> => {
    try {
      const response = await api.get(
        "/asistencias-diarias/por-disciplina-y-fecha",
        {
          params: { disciplinaId, fecha, page, size },
        }
      );
      return response.data;
    } catch (error: any) {
      console.error(
        "Error al obtener asistencias por disciplina y fecha:",
        error
      );
      toast.error("Error al obtener asistencias. Intente nuevamente.");
      throw error;
    }
  },

  eliminarAsistenciaDiaria: async (id: number): Promise<void> => {
    try {
      await api.delete(`/asistencias-diarias/${id}`);
      toast.success("Asistencia eliminada correctamente.");
    } catch (error: any) {
      console.error("Error al eliminar asistencia diaria:", error);
      toast.error("Error al eliminar asistencia. Intente nuevamente.");
      throw error;
    }
  },

  registrarAsistenciaDiaria: async (
    request: AsistenciaDiariaRegistroRequest
  ): Promise<AsistenciaDiariaResponse> => {
    try {
      if (!request.id) {
        throw new Error("El ID de asistencia es obligatorio.");
      }

      const response = await api.put(
        `/asistencias-diarias/${request.id}`, // ✅ Ahora usa el ID real
        request
      );
      return response.data;
    } catch (error: any) {
      console.error("Error al registrar asistencia diaria:", error);
      toast.error("No se pudo registrar la asistencia. Verifica los datos.");
      throw error;
    }
  },

  obtenerOCrearAsistenciaPorDisciplina: async (
    disciplinaId: number,
    mes: number,
    anio: number
  ): Promise<AsistenciaMensualDetalleResponse> => {
    try {
      const response = await api.get("/asistencias-mensuales/por-disciplina", {
        params: { disciplinaId, mes, anio },
      });
      if (!response.data) {
        throw new Error("No se encontró asistencia para esta disciplina.");
      }
      return response.data;
    } catch (error: any) {
      console.error("Error al obtener asistencia:", error);
      toast.error("Error al obtener asistencia. Intente nuevamente.");
      throw error;
    }
  },

  listarDisciplinasSimplificadas: async (): Promise<
    DisciplinaListadoResponse[]
  > => {
    try {
      const response = await api.get("/disciplinas/listado");
      return response.data;
    } catch (error: any) {
      console.error("Error al obtener disciplinas:", error);
      toast.error("Error al obtener disciplinas. Intente nuevamente.");
      throw error;
    }
  },
  registrarAsistenciaMensual: async (
    request: AsistenciaMensualRegistroRequest
  ): Promise<AsistenciaMensualDetalleResponse> => {
    try {
      const response = await api.post("/asistencias-mensuales", request);
      return response.data;
    } catch (error: any) {
      console.error("Error al registrar asistencia mensual:", error);
      toast.error("Error al registrar asistencia mensual. Intente nuevamente.");
      throw error;
    }
  },
  listarAsistenciasDiarias: async (): Promise<AsistenciaDiariaResponse[]> => {
    try {
      const response = await api.get("/asistencias-diarias");
      return response.data;
    } catch (error: any) {
      console.error("Error al obtener asistencias diarias:", error);
      toast.error("Error al obtener asistencias diarias. Intente nuevamente.");
      throw error;
    }
  },
};

export default asistenciasApi;
