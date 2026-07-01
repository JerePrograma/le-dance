import api from "./axiosConfig";
import type { ResumenCajaResponse } from "../types/types";

const obtenerResumen = async (desde: string, hasta: string, page = 0, size = 50): Promise<ResumenCajaResponse> =>
  (await api.get<ResumenCajaResponse>("/caja/resumen", { params: { desde, hasta, page, size } })).data;

export default { obtenerResumen };
