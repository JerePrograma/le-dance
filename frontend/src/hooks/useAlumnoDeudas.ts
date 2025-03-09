// src/hooks/useAlumnoDeudas.ts
import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import deudasApi from "../api/deudasApi";
import { toast } from "react-toastify";
import type { DeudasPendientesResponse } from "../types/types";

export const useAlumnoDeudas = (alumnoId: number) => {
  const query = useQuery<DeudasPendientesResponse, Error>({
    queryKey: ["deudas", alumnoId],
    queryFn: () => deudasApi.obtenerDeudasPendientes(alumnoId),
    enabled: Boolean(alumnoId),
  });

  useEffect(() => {
    if (query.error) {
      toast.error(query.error.message || "Error al cargar deudas");
    }
  }, [query.error]);

  // Si la respuesta no incluye alumnoId o alumnoNombre, se intenta tomarlo del objeto matrícula pendiente
  const dataWithAlumno = useMemo(() => {
    if (query.data) {
      let { alumnoId: debtAlumnoId, alumnoNombre } = query.data;
      if (!debtAlumnoId && query.data.matriculaPendiente?.alumnoId) {
        debtAlumnoId = query.data.matriculaPendiente.alumnoId;
        // Aquí podrías definir también un valor por defecto para alumnoNombre si tienes esa información en otro lado
        alumnoNombre = alumnoNombre || "Alumno desconocido";
      }
      return { ...query.data, alumnoId: debtAlumnoId, alumnoNombre };
    }
    return query.data;
  }, [query.data]);

  return { ...query, data: dataWithAlumno };
};
