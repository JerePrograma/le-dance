// src/cajaApi.ts
import api from "./axiosConfig";
import type {
  CajaDetalleDTO,
  CajaDiariaDTO,
  EgresoResponse,
  RendicionDTO,
  CobranzasDataResponse,
} from "../types/types";

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

  async agregarEgreso(
    fecha: string,
    monto: number,
    observaciones?: string,
    metodoPagoId: number = 1
  ): Promise<EgresoResponse> {
    const params = { monto, observaciones, metodoPagoId };
    const { data } = await api.post<EgresoResponse>(`/caja/dia/${fecha}/egresos`, null, { params });
    return data;
  },

  async actualizarEgreso(
    egresoId: number,
    payload: { fecha: string; monto: number; observaciones?: string; metodoPagoId?: number }
  ): Promise<EgresoResponse> {
    const { data } = await api.put<EgresoResponse>(`/caja/egresos/${egresoId}`, payload);
    return data;
  },

  async anularEgreso(egresoId: number): Promise<void> {
    await api.patch(`/caja/egresos/${egresoId}/anular`);
  },

  async obtenerRendicionGeneral(startDate: string, endDate: string): Promise<RendicionDTO> {
    const { data } = await api.get<RendicionDTO>("/caja/rendicion", {
      params: { startDate, endDate },
    });
    return data;
  },

  async generarRendicionMensual(): Promise<RendicionDTO> {
    const { data } = await api.post<RendicionDTO>("/caja/rendicion/generar");
    return data;
  },

  // Nuevo método para obtener los datos unificados de cobranzas.
  async obtenerDatosCobranzas(): Promise<CobranzasDataResponse> {
    const { data } = await api.get<CobranzasDataResponse>("/caja/datos-unificados");
    return data;
  },
};

export default cajaApi;
