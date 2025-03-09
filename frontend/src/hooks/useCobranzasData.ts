// src/hooks/useCobranzasData.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import alumnosApi from "../api/alumnosApi";
import pagosApi from "../api/pagosApi";
import metodosPagoApi from "../api/metodosPagoApi";
import conceptosApi from "../api/conceptosApi";
import type {
  AlumnoListadoResponse,
  StockResponse,
  MetodoPagoResponse,
  ConceptoResponse,
  DisciplinaDetalleResponse,
} from "../types/types";
import { toast } from "react-toastify";

export const useCobranzasData = () => {
  const alumnosQuery = useQuery<AlumnoListadoResponse[], Error>({
    queryKey: ["alumnos"],
    queryFn: alumnosApi.listar,
  });

  const disciplinasQuery = useQuery<DisciplinaDetalleResponse[], Error>({
    queryKey: ["disciplinas"],
    queryFn: pagosApi.listarDisciplinasBasicas,
  });

  const stocksQuery = useQuery<StockResponse[], Error>({
    queryKey: ["stocks"],
    queryFn: pagosApi.listarStocksBasicos,
  });

  const metodosPagoQuery = useQuery<MetodoPagoResponse[], Error>({
    queryKey: ["metodosPago"],
    queryFn: metodosPagoApi.listarMetodosPago,
  });

  const conceptosQuery = useQuery<ConceptoResponse[], Error>({
    queryKey: ["conceptos"],
    queryFn: conceptosApi.listarConceptos,
  });

  // Manejo de errores
  useEffect(() => {
    if (alumnosQuery.error)
      toast.error(alumnosQuery.error.message || "Error al cargar alumnos");
  }, [alumnosQuery.error]);

  useEffect(() => {
    if (disciplinasQuery.error)
      toast.error(disciplinasQuery.error.message || "Error al cargar disciplinas");
  }, [disciplinasQuery.error]);

  useEffect(() => {
    if (stocksQuery.error)
      toast.error(stocksQuery.error.message || "Error al cargar stocks");
  }, [stocksQuery.error]);

  useEffect(() => {
    if (metodosPagoQuery.error)
      toast.error(metodosPagoQuery.error.message || "Error al cargar métodos de pago");
  }, [metodosPagoQuery.error]);

  useEffect(() => {
    if (conceptosQuery.error)
      toast.error(conceptosQuery.error.message || "Error al cargar conceptos");
  }, [conceptosQuery.error]);

  return {
    alumnos: alumnosQuery.data || [],
    disciplinas: disciplinasQuery.data || [],
    stocks: stocksQuery.data || [],
    metodosPago: metodosPagoQuery.data || [],
    conceptos: conceptosQuery.data || [],
  };
};
