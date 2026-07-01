import api from "./axiosConfig";
import type { CargoResponse, Page } from "../types/types";

const listarPendientes = async (alumnoId: number, page = 0, size = 50): Promise<Page<CargoResponse>> =>
  (await api.get<Page<CargoResponse>>(`/cargos/alumno/${alumnoId}/pendientes`, { params: { page, size } })).data;

const obtener = async (id: number): Promise<CargoResponse> =>
  (await api.get<CargoResponse>(`/cargos/${id}`)).data;

export default { listarPendientes, obtener };
