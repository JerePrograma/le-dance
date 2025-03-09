// reporteMensualidadApi.ts
import api from "./axiosConfig";
import type { ReporteMensualidadDTO } from "../types/types";

const reporteMensualidadApi = {
    listarReporte: async (params: any): Promise<ReporteMensualidadDTO[]> => {
        const response = await api.get("/reportes/mensualidades/buscar", { params });
        // Asumiendo que el backend devuelve un array directamente
        return response.data;
    },

};

export default reporteMensualidadApi;
