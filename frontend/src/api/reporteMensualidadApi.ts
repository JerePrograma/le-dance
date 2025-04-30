// reporteMensualidadApi.ts
import type { DetallePagoResponse } from "../types/types";
import api from "./axiosConfig";

export interface ExportLiquidacionPayload {
  fechaInicio: string;
  fechaFin: string;
  disciplina?: string;
  profesor?: string;
  porcentaje: number;
  detalles: DetallePagoResponse[];
}

const reporteMensualidadApi = {
  listarReporte: async (params: any): Promise<DetallePagoResponse[]> => {
    const resp = await api.get("/reportes/mensualidades/buscar", { params });
    return resp.data;
  },

  exportarLiquidacion: async (
    payload: ExportLiquidacionPayload
  ): Promise<Blob> => {
    const resp = await api.post("/reportes/mensualidades/exportar", payload, {
      responseType: "blob",
    });
    return resp.data;
  },
};

export default reporteMensualidadApi;
