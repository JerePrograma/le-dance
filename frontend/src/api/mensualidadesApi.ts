// src/mensualidadesApi.ts
import api from "./axiosConfig";
import type {
  MensualidadRegistroRequest,
  MensualidadResponse,
} from "../types/types";

/**
 * Crea una nueva mensualidad.
 */
const crearMensualidad = async (request: MensualidadRegistroRequest): Promise<MensualidadResponse> => {
  const { data } = await api.post<MensualidadResponse>("/mensualidades", request);
  return data;
};

/**
 * Actualiza una mensualidad existente.
 */
const actualizarMensualidad = async (
  id: number,
  request: MensualidadRegistroRequest
): Promise<MensualidadResponse> => {
  const { data } = await api.put<MensualidadResponse>(`/mensualidades/${id}`, request);
  return data;
};

/**
 * Obtiene una mensualidad por su ID.
 */
const obtenerMensualidad = async (id: number): Promise<MensualidadResponse> => {
  const { data } = await api.get<MensualidadResponse>(`/mensualidades/${id}`);
  return data;
};

/**
 * Lista todas las mensualidades.
 */
const listarMensualidades = async (): Promise<MensualidadResponse[]> => {
  const { data } = await api.get<MensualidadResponse[]>("/mensualidades");
  return data;
};

/**
 * Lista las mensualidades asociadas a una inscripción.
 */
const listarMensualidadesPorInscripcion = async (
  inscripcionId: number
): Promise<MensualidadResponse[]> => {
  const { data } = await api.get<MensualidadResponse[]>(`/mensualidades/inscripcion/${inscripcionId}`);
  return data;
};

/**
 * Elimina (baja lógica o física) una mensualidad.
 */

const eliminarMensualidad = async (id: number): Promise<void> => {
  await api.delete(`/mensualidades/${id}`);
};
const listarPorInscripcion = async (inscripcionId: number): Promise<MensualidadResponse[]> => {
  const { data } = await api.get<MensualidadResponse[]>(`/mensualidades/inscripcion/${inscripcionId}`);
  return data;
};

/**
 * NUEVO: Genera (o actualiza) las mensualidades para el mes vigente.
 */
const generarMensualidadesParaMesVigente = async (): Promise<MensualidadResponse[]> => {
  const { data } = await api.post<MensualidadResponse[]>("/mensualidades/generar-mensualidades");
  return data;
};

const mensualidadesApi = {
  crearMensualidad,
  actualizarMensualidad,
  obtenerMensualidad,
  listarMensualidades,
  listarMensualidadesPorInscripcion,
  eliminarMensualidad,
  listarPorInscripcion,
  generarMensualidadesParaMesVigente,
};

export default mensualidadesApi;
