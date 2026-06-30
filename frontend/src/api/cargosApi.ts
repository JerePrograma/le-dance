import api from "./axiosConfig";
import type { CargoResponse } from "../types/types";

const listarPendientes = async (alumnoId: number): Promise<CargoResponse[]> =>
  (await api.get<CargoResponse[]>(`/cargos/alumno/${alumnoId}/pendientes`)).data;

const obtener = async (id: number): Promise<CargoResponse> =>
  (await api.get<CargoResponse>(`/cargos/${id}`)).data;

export default { listarPendientes, obtener };
