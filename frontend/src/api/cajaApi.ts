// src/cajaApi.ts
import {
  CajaDetalleDTO,
  CajaDiariaDTO,
  Egreso,
  RendicionDTO
} from "../types/types";
import api from "./axiosConfig";

const cajaApi = {
  async obtenerPlanillaGeneral(startDate: string, endDate: string): Promise<CajaDiariaDTO[]> {
    const { data } = await api.get<CajaDiariaDTO[]>("/caja/planilla", {
      params: { startDate, endDate },
    });
    return data;
  },

  async obtenerCajaDiaria(fecha: string): Promise<CajaDetalleDTO> {
    const { data } = await api.get<CajaDetalleDTO>(`/caja/dia/${fecha}`);
    return data;
  },

  async agregarEgreso(fecha: string, monto: number, observaciones?: string, metodoPagoId: number = 1): Promise<Egreso> {
    const params = { monto, observaciones, metodoPagoId };
    const { data } = await api.post<Egreso>(`/caja/dia/${fecha}/egresos`, null, { params });
    return data;
  },

  async obtenerRendicionGeneral(startDate: string, endDate: string): Promise<RendicionDTO> {
    const { data } = await api.get<RendicionDTO>("/caja/rendicion", {
      params: { startDate, endDate },
    });
    return data;
  },

  async anularEgreso(egresoId: number): Promise<void> {
    await api.patch(`/caja/egresos/${egresoId}/anular`);
  },

  async actualizarEgreso(
    egresoId: number,
    payload: { fecha: string; monto: number; observaciones?: string; metodoPagoId?: number }
  ): Promise<Egreso> {
    const { data } = await api.put<Egreso>(`/caja/egresos/${egresoId}`, payload);
    return data;
  },

  // NUEVO: Endpoint para generar la rendición mensual (se supone que el back lo implementó)
  async generarRendicionMensual(): Promise<RendicionDTO> {
    const { data } = await api.post<RendicionDTO>("/caja/rendicion/generar");
    return data;
  },
};

export default cajaApi;
