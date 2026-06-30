import api from "./axiosConfig";
import type { EgresoRegistroRequest, EgresoResponse } from "../types/types";

const registrarEgreso = async (request: EgresoRegistroRequest): Promise<EgresoResponse> =>
  (await api.post<EgresoResponse>("/egresos", request)).data;
const listarEgresos = async (): Promise<EgresoResponse[]> =>
  (await api.get<EgresoResponse[]>("/egresos")).data;
const anularEgreso = async (id: number, motivo: string): Promise<EgresoResponse> =>
  (await api.post<EgresoResponse>(`/egresos/${id}/anulacion`, {
    motivo,
    idempotencyKey: crypto.randomUUID(),
  })).data;

export default { registrarEgreso, listarEgresos, anularEgreso };
