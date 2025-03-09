// src/hooks/useInscripcionesActivas.ts
import { useQuery } from "@tanstack/react-query";
import inscripcionesApi from "../api/inscripcionesApi";
import type { InscripcionResponse } from "../types/types";

export const useInscripcionesActivas = (alumnoId: number) =>
    useQuery<InscripcionResponse[], Error>({
        queryKey: ["inscripciones", alumnoId],
        queryFn: () => inscripcionesApi.obtenerInscripcionesActivas(alumnoId),
        enabled: Boolean(alumnoId),
    });
