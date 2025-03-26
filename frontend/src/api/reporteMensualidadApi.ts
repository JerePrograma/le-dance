// reporteMensualidadApi.ts
import { DetallePagoResponse } from "../types/types";
import api from "./axiosConfig";

const reporteMensualidadApi = {
    listarReporte: async (params: any): Promise<DetallePagoResponse[]> => {
        const response = await api.get("/reportes/mensualidades/buscar", { params });
        // Asumiendo que el backend devuelve un array directamente
        return response.data;
    },

};

export default reporteMensualidadApi;
