import api from "./axiosConfig";
import {
  DisciplinaRequest,
  DisciplinaResponse,
  AlumnoListadoResponse,
  ProfesorListadoResponse,
} from "../types/types";

const disciplinasApi = {
  listarDisciplinas: async (): Promise<DisciplinaResponse[]> => {
    const response = await api.get("/api/disciplinas");
    return response.data;
  },

  obtenerDisciplinaPorId: async (id: number): Promise<DisciplinaResponse> => {
    const response = await api.get(`/api/disciplinas/${id}`);
    return response.data;
  },

  crearDisciplina: async (
    disciplina: DisciplinaRequest
  ): Promise<DisciplinaResponse> => {
    const response = await api.post("/api/disciplinas", disciplina);
    return response.data;
  },

  actualizarDisciplina: async (
    id: number,
    disciplina: DisciplinaRequest
  ): Promise<DisciplinaResponse> => {
    const response = await api.put(`/api/disciplinas/${id}`, disciplina);
    return response.data;
  },

  eliminarDisciplina: async (id: number): Promise<string> => {
    const response = await api.put(`/api/disciplinas/${id}`, { activo: false });
    return response.data;
  },

  /** 🔹 Obtener alumnos de una disciplina */
  obtenerAlumnosDeDisciplina: async (
    disciplinaId: number
  ): Promise<AlumnoListadoResponse[]> => {
    const response = await api.get(`/api/disciplinas/${disciplinaId}/alumnos`);
    return response.data;
  },

  /** 🔹 Obtener profesores de una disciplina */
  obtenerProfesoresDeDisciplina: async (
    disciplinaId: number
  ): Promise<ProfesorListadoResponse[]> => {
    const response = await api.get(
      `/api/disciplinas/${disciplinaId}/profesores`
    );
    return response.data;
  },
};

export default disciplinasApi;
