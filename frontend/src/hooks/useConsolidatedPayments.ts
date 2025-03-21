// src/hooks/useConsolidatedDetails.ts
import { useMemo } from "react";
import type { DeudasPendientesResponse } from "../types/types";

export interface ConsolidatedDetail {
  concepto: string;
  codigoConcepto: number;
  cantidad: number;
  valorBase: number;
  bonificacionId?: number | string;
  recargoId?: number | string;
  importe: number;
  aCobrar: number;
}

export const useConsolidatedDetails = (
  deudaData: DeudasPendientesResponse | undefined
): ConsolidatedDetail[] => {
  return useMemo(() => {
    if (!deudaData) return [];
    const detailMap: Record<string, ConsolidatedDetail> = {};
    deudaData.pagosPendientes?.forEach((pago) => {
      pago.detallePagos?.forEach((det) => {
        const key = det.concepto;
        const cantidad = Number(det.cuota) || 1;
        if (!detailMap[key]) {
          detailMap[key] = {
            concepto: det.concepto,
            codigoConcepto: det.id, // O si prefieres, det.codigoConcepto
            cantidad,
            valorBase: det.valorBase,
            bonificacionId: det.bonificacion?.id,
            recargoId: det.recargo?.id,
            importe: det.importe,
            aCobrar: det.aCobrar,
          };
        } else {
          detailMap[key].cantidad += cantidad;
          detailMap[key].valorBase += det.valorBase;
          detailMap[key].importe += det.importe;
          detailMap[key].aCobrar += det.aCobrar;
        }
      });
    });
    return Object.values(detailMap);
  }, [deudaData]);
};
