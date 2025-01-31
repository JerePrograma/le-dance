// inscripcionesApi.ts
import api from "./axiosConfig";
import { InscripcionRequest, InscripcionResponse } from "../types/types";

const inscripcionesApi = {
  // Lista todas
  listarInscripciones: async (): Promise<InscripcionResponse[]> => {
    const response = await api.get("/api/inscripciones");
    return response.data;
  },

  // Lista por alumno
  listarInscripcionesPorAlumno: async (alumnoId: number) => {
    const response = await api.get(`/api/inscripciones?alumnoId=${alumnoId}`);
    return response.data as InscripcionResponse[];
  },

  // Crear
  crearInscripcion: async (
    request: InscripcionRequest
  ): Promise<InscripcionResponse> => {
    const response = await api.post("/api/inscripciones", request);
    return response.data;
  },

  // Actualizar (nuevo método)
  actualizarInscripcion: async (
    id: number,
    request: InscripcionRequest
  ): Promise<InscripcionResponse> => {
    const response = await api.put(`/api/inscripciones/${id}`, request);
    return response.data;
  },

  // Eliminar
  eliminarInscripcion: async (id: number): Promise<string> => {
    const response = await api.put(`/api/inscripciones/${id}`, {
      activo: false,
    });
    return response.data;
  },

  // Obtener por ID
  obtenerInscripcionPorId: async (id: number): Promise<InscripcionResponse> => {
    const response = await api.get(`/api/inscripciones/${id}`);
    return response.data;
  },
};

export default inscripcionesApi;
