// useUltimoPago.ts
import { useState, useEffect } from "react";
import pagosApi from "../api/pagosApi";
import type { PagoResponse } from "../types/types";

export const useUltimoPago = (alumnoId: number | null) => {
  const [ultimoPago, setUltimoPago] = useState<PagoResponse | null>(null);

  useEffect(() => {
    if (alumnoId) {
      pagosApi
        .obtenerUltimoPagoPorAlumno(alumnoId)
        .then(setUltimoPago)
        .catch((err) => {
          console.error("Error al cargar último pago:", err);
          setUltimoPago(null);
        });
    }
  }, [alumnoId]);

  return ultimoPago;
};
