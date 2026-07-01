import api from "./axiosConfig";
import type { EgresoRegistroRequest, EgresoResponse, Page } from "../types/types";

const registrarEgreso = async (request: EgresoRegistroRequest): Promise<EgresoResponse> =>
  (await api.post<EgresoResponse>("/egresos", request)).data;
const listarEgresos = async (page = 0, size = 50): Promise<Page<EgresoResponse>> =>
  (await api.get<Page<EgresoResponse>>("/egresos", { params: { page, size } })).data;
const anularEgreso = async (id: number, motivo: string): Promise<EgresoResponse> =>
  (await api.post<EgresoResponse>(`/egresos/${id}/anulacion`, {
    motivo,
    idempotencyKey: crypto.randomUUID(),
  })).data;

export default { registrarEgreso, listarEgresos, anularEgreso };
