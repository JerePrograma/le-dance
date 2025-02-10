import { useState, useEffect, useCallback } from "react";
import { toast } from "react-toastify";
import asistenciasApi from "../api/asistenciasApi";
import type {
  AsistenciaMensualDetalleResponse,
  AsistenciaMensualListadoResponse,
} from "../types/types";

export const useAsistencias = (
  disciplinaId?: number,
  mes?: number,
  anio?: number
) => {
  const [asistenciaMensual, setAsistenciaMensual] =
    useState<AsistenciaMensualDetalleResponse | null>(null);
  const [asistencias, setAsistencias] = useState<
    AsistenciaMensualListadoResponse[]
  >([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cargarAsistencias = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await asistenciasApi.listarAsistenciasMensuales(
        undefined,
        disciplinaId,
        mes,
        anio
      );
      setAsistencias(response);
    } catch (err) {
      setError("Error al cargar asistencias.");
      toast.error("No se pudieron cargar las asistencias.");
    } finally {
      setLoading(false);
    }
  }, [disciplinaId, mes, anio]);

  const cargarAsistenciaMensual = useCallback(async () => {
    if (!disciplinaId || !mes || !anio) return;
    setLoading(true);
    setError(null);
    try {
      const response =
        await asistenciasApi.obtenerOCrearAsistenciaPorDisciplina(
          disciplinaId,
          mes,
          anio
        );
      setAsistenciaMensual(response);
    } catch (err) {
      setError("Error al cargar asistencia mensual.");
      toast.error("No se pudo cargar la asistencia mensual.");
    } finally {
      setLoading(false);
    }
  }, [disciplinaId, mes, anio]);

  useEffect(() => {
    if (disciplinaId) {
      cargarAsistencias();
      cargarAsistenciaMensual();
    }
  }, [cargarAsistencias, cargarAsistenciaMensual]);

  return {
    asistenciaMensual,
    asistencias,
    loading,
    error,
    cargarAsistencias,
    cargarAsistenciaMensual,
  };
};
