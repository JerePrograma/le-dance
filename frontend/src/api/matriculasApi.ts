import api from "./axiosConfig";
import type { MatriculaResponse } from "../types/types";

const obtenerMatricula = async (alumnoId: number, anio: number): Promise<MatriculaResponse> =>
  (await api.get<MatriculaResponse>(`/matriculas/alumno/${alumnoId}`, { params: { anio } })).data;
const generarMatricula = async (alumnoId: number, anio: number): Promise<MatriculaResponse> =>
  (await api.post<MatriculaResponse>(`/matriculas/alumno/${alumnoId}`, null, { params: { anio } })).data;
const anularMatricula = async (id: number): Promise<MatriculaResponse> =>
  (await api.post<MatriculaResponse>(`/matriculas/${id}/anulacion`)).data;

export default { obtenerMatricula, generarMatricula, anularMatricula };
