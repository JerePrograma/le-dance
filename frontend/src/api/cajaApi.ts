import api from "./axiosConfig";
import type { ResumenCajaResponse } from "../types/types";

const obtenerResumen = async (desde: string, hasta: string): Promise<ResumenCajaResponse> =>
  (await api.get<ResumenCajaResponse>("/caja/resumen", { params: { desde, hasta } })).data;

export default { obtenerResumen };
