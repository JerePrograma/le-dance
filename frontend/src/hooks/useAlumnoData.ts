// src/hooks/useAlumnoData.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "react-toastify";
import alumnosApi from "../api/alumnosApi";
import type { AlumnoDataResponse } from "../types/types";

/**
 * Se asume que AlumnoDataResponse tiene la siguiente estructura:
 * {
 *   alumno: AlumnoDetalleResponse,
 *   inscripcionesActivas: InscripcionResponse[],
 *   deudas: DeudasPendientesResponse,
 *   ultimoPago: PagoResponse | null
 * }
 */
export const useAlumnoData = (alumnoId: number) => {
    const isAlumnoValid = alumnoId > 0;

    const alumnoDataQuery = useQuery<AlumnoDataResponse, Error>({
        queryKey: ["alumnoData", alumnoId],
        queryFn: () => alumnosApi.obtenerDatosAlumno(alumnoId),
        enabled: isAlumnoValid,
        staleTime: Infinity,
        refetchOnWindowFocus: false,
    });

    useEffect(() => {
        if (alumnoDataQuery.error) {
            toast.error(
                alumnoDataQuery.error.message || "Error al cargar los datos del alumno"
            );
        }
    }, [alumnoDataQuery.error]);

    return {
        data: alumnoDataQuery.data,
        isLoading: alumnoDataQuery.isLoading,
        error: alumnoDataQuery.error,
    };
};
