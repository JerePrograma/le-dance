import api from "./axiosConfig";
import type {
  ProfesorRegistroRequest,
  ProfesorModificacionRequest,
  ProfesorDetalleResponse,
  ProfesorListadoResponse,
  DisciplinaListadoResponse,
  AlumnoResponse,
} from "../types/types";

/**
 * API client para Profesores
 */
const profesoresApi = {
  /**
   * Registra un nuevo profesor
   */
  async registrarProfesor(
    payload: ProfesorRegistroRequest
  ): Promise<ProfesorDetalleResponse> {
    const { data } = await api.post<ProfesorDetalleResponse>(
      "/profesores",
      payload
    );
    return data;
  },

  /**
   * Obtiene el detalle de un profesor por su ID
   */
  async obtenerProfesorPorId(id: number): Promise<ProfesorDetalleResponse> {
    const { data } = await api.get<ProfesorDetalleResponse>(
      `/profesores/${id}`
    );
    return data;
  },

  /**
   * Lista todos los profesores
   * @param activos si true filtra sólo activos, de lo contrario todos
   */
  async listarProfesores(activos = false): Promise<ProfesorListadoResponse[]> {
    const endpoint = activos ? "/profesores/activos" : "/profesores";
    const { data } = await api.get<ProfesorListadoResponse[]>(endpoint);
    return data;
  },

  /**
   * Busca profesores por nombre (parcial)
   */
  async buscarPorNombre(nombre: string): Promise<ProfesorListadoResponse[]> {
    const { data } = await api.get<ProfesorListadoResponse[]>(
      "/profesores/buscar",
      { params: { nombre } }
    );
    return data;
  },

  /**
   * Actualiza un profesor existente
   */
  async actualizarProfesor(
    id: number,
    payload: ProfesorModificacionRequest
  ): Promise<ProfesorDetalleResponse> {
    const { data } = await api.put<ProfesorDetalleResponse>(
      `/profesores/${id}`,
      payload
    );
    return data;
  },

  /**
   * Elimina un profesor por su ID
   */
  async eliminarProfesor(id: number): Promise<void> {
    await api.delete(`/profesores/${id}`);
  },

  /**
   * Obtiene las disciplinas asignadas a un profesor
   */
  async obtenerDisciplinasDeProfesor(
    profesorId: number
  ): Promise<DisciplinaListadoResponse[]> {
    const { data } = await api.get<DisciplinaListadoResponse[]>(
      `/profesores/${profesorId}/disciplinas`
    );
    return data;
  },

  /**
   * Obtiene los alumnos de las disciplinas impartidas por un profesor
   */
  async findAlumnosPorProfesor(profesorId: number): Promise<AlumnoResponse[]> {
    const { data } = await api.get<AlumnoResponse[]>(
      `/profesores/${profesorId}/alumnos`
    );
    return data;
  },
};

export default profesoresApi;
