// src/hooks/useCobranzasData.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "react-toastify";
import cajaApi from "../api/cajaApi";
import type {
  CobranzasDataResponse,
} from "../types/types";

export const useCobranzasData = () => {
  const cobranzasQuery = useQuery<CobranzasDataResponse, Error>({
    queryKey: ["cobranzasData"],
    queryFn: cajaApi.obtenerDatosCobranzas, // Asegúrate de que este endpoint devuelva bonificaciones y recargos también
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  
  useEffect(() => {
    if (cobranzasQuery.error) {
      toast.error(
        cobranzasQuery.error.message || "Error al cargar los datos de cobranzas"
      );
    }
  }, [cobranzasQuery.error]);
  
  return cobranzasQuery.data || {
    alumnos: [],
    disciplinas: [],
    stocks: [],
    metodosPago: [],
    conceptos: [],
    bonificaciones: [],
    recargos: [],
  };
};
