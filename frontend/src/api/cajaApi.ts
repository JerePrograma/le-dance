// src/cajaApi.ts

import api from "./axiosConfig";

// -----------------------------------------------------------------------------
//  INTERFACES (puedes moverlas a un archivo separate e importarlas si prefieres)
// -----------------------------------------------------------------------------

export interface AlumnoMin {
  id: number;
  nombre: string;
  apellido: string;
}

export interface Pago {
  id: number;
  fecha: string; // "2025-02-26"
  monto: number;
  observaciones?: string;
  alumno?: AlumnoMin; // { id, nombre, apellido }
  // método de pago, etc., según tu back
}

export interface Egreso {
  id: number;
  fecha: string;
  monto: number;
  observaciones?: string;
  // etc...
}

export interface CajaDiariaDTO {
  fecha: string; // "2025-02-26"
  rangoRecibos: string; // "Recibo #10 al #12"
  totalEfectivo: number;
  totalDebito: number;
  totalEgresos: number;
  totalNeto: number;
}

export interface CajaDetalleDTO {
  pagosDelDia: Pago[];
  egresosDelDia: Egreso[];
}

export interface RendicionDTO {
  pagos: Pago[];
  egresos: Egreso[];
  totalEfectivo: number;
  totalDebito: number;
  totalEgresos: number;
}

// -----------------------------------------------------------------------------
//  SERVICIOS
// -----------------------------------------------------------------------------
const cajaApi = {
  /**
   * 1) Obtener la planilla general de caja en un rango de fechas
   *    GET /caja/planilla?startDate=...&endDate=...
   */
  async obtenerPlanillaGeneral(
    startDate: string,
    endDate: string
  ): Promise<CajaDiariaDTO[]> {
    const { data } = await api.get<CajaDiariaDTO[]>("/caja/planilla", {
      params: { startDate, endDate },
    });
    return data;
  },

  /**
   * 2) Obtener la caja diaria (detalle) para una fecha dada
   *    GET /caja/dia/{fecha}
   */
  async obtenerCajaDiaria(fecha: string): Promise<CajaDetalleDTO> {
    const { data } = await api.get<CajaDetalleDTO>(`/caja/dia/${fecha}`);
    return data;
  },

  /**
   * 3) Agregar un egreso en una fecha dada
   *    POST /caja/dia/{fecha}/egresos?monto=...&observaciones=...&metodoPagoId=...
   */
  async agregarEgreso(
    fecha: string,
    monto: number,
    observaciones?: string,
    metodoPagoId: number = 1 // Por defecto 1 => EFECTIVO, si tu back lo maneja así
  ): Promise<Egreso> {
    const params = {
      monto,
      observaciones,
      metodoPagoId,
    };
    const { data } = await api.post<Egreso>(
      `/caja/dia/${fecha}/egresos`,
      null,
      {
        params,
      }
    );
    return data;
  },

  /**
   * 4) Obtener la rendición general de caja en un rango de fechas
   *    GET /caja/rendicion?startDate=...&endDate=...
   */
  async obtenerRendicionGeneral(
    startDate: string,
    endDate: string
  ): Promise<RendicionDTO> {
    const { data } = await api.get<RendicionDTO>("/caja/rendicion", {
      params: { startDate, endDate },
    });
    return data;
  },

  /**
   * 5) Anular (dar de baja) un Egreso existente
   *    PATCH /caja/egresos/{egresoId}/anular
   */
  async anularEgreso(egresoId: number): Promise<void> {
    await api.patch(`/caja/egresos/${egresoId}/anular`);
  },

  /**
   * 6) Actualizar un Egreso (fecha, monto, observaciones, metodoPagoId)
   *    PUT /caja/egresos/{egresoId}
   */
  async actualizarEgreso(
    egresoId: number,
    payload: {
      fecha: string;
      monto: number;
      observaciones?: string;
      metodoPagoId?: number;
    }
  ): Promise<Egreso> {
    const { data } = await api.put<Egreso>(
      `/caja/egresos/${egresoId}`,
      payload
    );
    return data;
  },
};

export default cajaApi;
