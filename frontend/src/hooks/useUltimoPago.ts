// src/hooks/useUltimoPago.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import pagosApi from "../api/pagosApi";
import type { PagoResponse } from "../types/types";
import { toast } from "react-toastify";

export const useUltimoPago = (alumnoId: number | null) => {
  const query = useQuery<PagoResponse, Error>({
    queryKey: ["ultimoPago", alumnoId],
    queryFn: () => pagosApi.obtenerUltimoPagoPorAlumno(alumnoId!),
    enabled: Boolean(alumnoId),
  });

  useEffect(() => {
    if (query.error) {
      toast.error(query.error.message || "Error al cargar último pago");
    }
  }, [query.error]);

  return query;
};
