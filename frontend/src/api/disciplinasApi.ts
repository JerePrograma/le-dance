import api from "./axiosConfig";
import type {
  DisciplinaRegistroRequest,
  DisciplinaModificacionRequest,
  DisciplinaDetalleResponse,
  DisciplinaListadoResponse,
  AlumnoListadoResponse,
  ProfesorListadoResponse,
} from "../types/types";

const disciplinasApi = {
  registrarDisciplina: async (
    disciplina: DisciplinaRegistroRequest
  ): Promise<DisciplinaDetalleResponse> => {
    const response = await api.post("/api/disciplinas", disciplina);
    return response.data;
  },

  listarDisciplinas: async (): Promise<DisciplinaDetalleResponse[]> => {
    const response = await api.get("/api/disciplinas");
    return response.data;
  },

  obtenerDisciplinaPorId: async (
    id: number
  ): Promise<DisciplinaDetalleResponse> => {
    const response = await api.get(`/api/disciplinas/${id}`);
    return response.data;
  },

  actualizarDisciplina: async (
    id: number,
    disciplina: DisciplinaModificacionRequest
  ): Promise<DisciplinaDetalleResponse> => {
    const response = await api.put(`/api/disciplinas/${id}`, disciplina);
    return response.data;
  },

  darBajaDisciplina: async (id: number): Promise<void> => {
    await api.delete(`/api/disciplinas/${id}`);
  },

  listarDisciplinasSimplificadas: async (): Promise<
    DisciplinaListadoResponse[]
  > => {
    const response = await api.get("/api/disciplinas/listado");
    return response.data;
  },

  obtenerDisciplinasPorFecha: async (
    fecha: string
  ): Promise<DisciplinaListadoResponse[]> => {
    const response = await api.get(
      `/api/disciplinas/por-fecha?fecha=${encodeURIComponent(fecha)}`
    );
    return response.data;
  },

  obtenerAlumnosDeDisciplina: async (
    disciplinaId: number
  ): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(`/api/disciplinas/${disciplinaId}/alumnos`);
    return response.data;
  },

  obtenerProfesorDeDisciplina: async (
    disciplinaId: number
  ): Promise<ProfesorListadoResponse> => {
    const response = await api.get(`/api/disciplinas/${disciplinaId}/profesor`);
    return response.data;
  },

  obtenerDisciplinasPorHorario: async (
    horario: string
  ): Promise<DisciplinaListadoResponse[]> => {
    const response = await api.get(
      `/api/disciplinas/por-horario?horario=${encodeURIComponent(horario)}`
    );
    return response.data;
  },
};

export default disciplinasApi;
