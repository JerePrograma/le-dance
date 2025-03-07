// useUltimoPago.ts
import { useState, useEffect } from "react";
import pagosApi from "../api/pagosApi";
import type { PagoResponse } from "../types/types";
import { toast } from "react-toastify";

export const useUltimoPago = (alumnoId: number | null) => {
  const [ultimoPago, setUltimoPago] = useState<PagoResponse | null>(null);

  useEffect(() => {
    if (alumnoId) {
      pagosApi
        .obtenerUltimoPagoPorAlumno(alumnoId)
        .then(setUltimoPago)
        .catch((err) => {
          toast.error("Error al cargar ultimo pago:", err);
          setUltimoPago(null);
        });
    }
  }, [alumnoId]);

  return ultimoPago;
};
