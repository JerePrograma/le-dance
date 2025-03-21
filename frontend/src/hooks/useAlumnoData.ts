// src/hooks/useAlumnoData.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "react-toastify";
import type { AlumnoDataResponse } from "../types/types";
import alumnosApi from "../api/alumnosApi";

export const useAlumnoData = (alumnoId: number) => {
  const query = useQuery<AlumnoDataResponse, Error>({
    queryKey: ["alumnoData", alumnoId],
    queryFn: () => alumnosApi.obtenerDatosAlumno(alumnoId),
    enabled: Boolean(alumnoId),
  });

  useEffect(() => {
    if (query.error) {
      toast.error(query.error.message || "Error al cargar datos del alumno");
    }
  }, [query.error]);

  return query;
};
