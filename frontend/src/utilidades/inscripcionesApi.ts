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
    // Por ejemplo: GET /api/inscripciones?alumnoId=...
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

  // ...
  eliminarInscripcion: async (id: number) => {
    await api.delete(`/api/inscripciones/${id}`);
  },

  obtenerInscripcionPorId: async (id: number): Promise<InscripcionResponse> => {
    const response = await api.get(`/api/inscripciones/${id}`);
    return response.data;
  },
};

export default inscripcionesApi;
