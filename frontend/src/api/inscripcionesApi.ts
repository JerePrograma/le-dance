// src/inscripcionesApi.ts
import api from "./axiosConfig";
import type {
  InscripcionRegistroRequest,
  InscripcionModificacionRequest,
  InscripcionResponse,
} from "../types/types";

const crear = async (
  request: InscripcionRegistroRequest
): Promise<InscripcionResponse> => {
  const response = await api.post("/inscripciones", request);
  return response.data;
};

const listar = async (alumnoId?: number): Promise<InscripcionResponse[]> => {
  const url = alumnoId
    ? `/inscripciones?alumnoId=${alumnoId}`
    : "/inscripciones";
  const response = await api.get(url);
  return response.data;
};

const obtenerPorId = async (id: number): Promise<InscripcionResponse> => {
  const response = await api.get(`/inscripciones/${id}`);
  return response.data;
};

const listarPorDisciplina = async (
  disciplinaId: number
): Promise<InscripcionResponse[]> => {
  const response = await api.get(`/inscripciones/disciplina/${disciplinaId}`);
  return response.data;
};

const actualizar = async (
  id: number,
  request: InscripcionModificacionRequest
): Promise<InscripcionResponse> => {
  const response = await api.put(`/inscripciones/${id}`, request);
  return response.data;
};

const eliminar = async (id: number): Promise<void> => {
  await api.delete(`/inscripciones/${id}`);
};

const obtenerInscripcionActiva = async (
  alumnoId: number
): Promise<InscripcionResponse> => {
  const { data } = await api.get<InscripcionResponse>(
    `/inscripciones/alumno/${alumnoId}`
  );
  return data;
};

const inscripcionesApi = {
  crear,
  listar,
  obtenerPorId,
  listarPorDisciplina,
  actualizar,
  eliminar,
  obtenerInscripcionActiva,
};

export default inscripcionesApi;
