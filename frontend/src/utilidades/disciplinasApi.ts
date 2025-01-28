import api from "./axiosConfig";
import {
  DisciplinaRequest,
  DisciplinaResponse,
  AlumnoRequest,
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
    const response = await api.delete(`/api/disciplinas/${id}`);
    return response.data;
  },

  inscribirAlumno: async (
    disciplinaId: number,
    alumno: AlumnoRequest
  ): Promise<string> => {
    const response = await api.post(
      `/api/disciplinas/${disciplinaId}/inscribir-alumno`,
      alumno
    );
    return response.data;
  },
};

export default disciplinasApi;
