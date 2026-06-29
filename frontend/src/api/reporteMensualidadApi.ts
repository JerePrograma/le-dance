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

export interface ReporteMensualidadParams {
  fechaDesde?: string;
  fechaHasta?: string;
  fechaInicio?: string;
  fechaFin?: string;
  disciplinaNombre?: string;
  profesorNombre?: string;
  alumnoId?: number;
  disciplinaId?: number;
  profesorId?: number;
  estado?: string;
  page?: number;
}

const reporteMensualidadApi = {
  listarReporte: async (params: ReporteMensualidadParams): Promise<DetallePagoResponse[]> => {
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
