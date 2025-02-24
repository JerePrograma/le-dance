import api from "./axiosConfig";
import type {
  MatriculaRegistroRequest,
  MatriculaModificacionRequest,
  MatriculaResponse,
} from "../types/types";

const obtenerMatricula = async (
  alumnoId: number
): Promise<MatriculaResponse> => {
  const { data } = await api.get<MatriculaResponse>(
    `/api/matriculas/${alumnoId}`
  );
  return data;
};

const actualizarMatricula = async (
  matriculaId: number,
  request: MatriculaModificacionRequest
): Promise<MatriculaResponse> => {
  const { data } = await api.put<MatriculaResponse>(
    `/api/matriculas/${matriculaId}`,
    request
  );
  return data;
};

const matriculasApi = {
  obtenerMatricula,
  actualizarMatricula,
};

export default matriculasApi;
