// src/hooks/useAlumnoData.ts
import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import deudasApi from "../api/deudasApi";
import pagosApi from "../api/pagosApi";
import { toast } from "react-toastify";
import type { DeudasPendientesResponse, PagoResponse } from "../types/types";

export const useAlumnoData = (alumnoId: number) => {
    // Consulta de deudas pendientes
    const deudasQuery = useQuery<DeudasPendientesResponse, Error>({
        queryKey: ["deudas", alumnoId],
        queryFn: () => deudasApi.obtenerDeudasPendientes(alumnoId),
        enabled: Boolean(alumnoId),
    });

    // Consulta del último pago
    const ultimoPagoQuery = useQuery<PagoResponse, Error>({
        queryKey: ["ultimoPago", alumnoId],
        queryFn: () => pagosApi.obtenerUltimoPagoPorAlumno(alumnoId),
        enabled: Boolean(alumnoId),
    });

    // Manejo de errores en ambas consultas
    useEffect(() => {
        if (deudasQuery.error) {
            toast.error(deudasQuery.error.message || "Error al cargar deudas");
        }
    }, [deudasQuery.error]);

    useEffect(() => {
        if (ultimoPagoQuery.error) {
            toast.error(ultimoPagoQuery.error.message || "Error al cargar último pago");
        }
    }, [ultimoPagoQuery.error]);

    // Procesamos los datos de deudas para asegurar que alumnoId y alumnoNombre sean consistentes
    const deudasData = useMemo(() => {
        if (deudasQuery.data) {
            let { alumnoId: debtAlumnoId, alumnoNombre } = deudasQuery.data;
            // Si la deuda no trae alumnoId, se intenta extraerlo de la matrícula pendiente
            if (!debtAlumnoId && deudasQuery.data.matriculaPendiente?.alumnoId) {
                debtAlumnoId = deudasQuery.data.matriculaPendiente.alumnoId;
                alumnoNombre = alumnoNombre || "Alumno desconocido";
            }
            return { ...deudasQuery.data, alumnoId: debtAlumnoId, alumnoNombre };
        }
        return deudasQuery.data;
    }, [deudasQuery.data]);

    // Indicador general de carga
    const isLoading = deudasQuery.isLoading || ultimoPagoQuery.isLoading;

    return {
        deudas: deudasData,
        ultimoPago: ultimoPagoQuery.data,
        isLoading,
        error: deudasQuery.error || ultimoPagoQuery.error,
    };
};
