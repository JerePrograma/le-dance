// src/hooks/useAlumnoData.ts
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import deudasApi from "../api/deudasApi";
import pagosApi from "../api/pagosApi";
import { toast } from "react-toastify";
import type { DeudasPendientesResponse, PagoResponse } from "../types/types";

export const useAlumnoData = (alumnoId: number) => {
    const isAlumnoValid = alumnoId > 0;

    const deudasQuery = useQuery<DeudasPendientesResponse, Error>({
        queryKey: ["deudas", alumnoId],
        queryFn: () => deudasApi.obtenerDeudasPendientes(alumnoId),
        enabled: isAlumnoValid,
        staleTime: Infinity,
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });

    const ultimoPagoQuery = useQuery<PagoResponse, Error>({
        queryKey: ["ultimoPago", alumnoId],
        queryFn: () => pagosApi.obtenerUltimoPagoPorAlumno(alumnoId),
        enabled: isAlumnoValid,
        staleTime: Infinity,
        refetchOnMount: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
    });

    // Calculamos el pago pendiente activo (aquellos con saldoRestante > 0)
    const pagoPendienteActivo = deudasQuery.data?.pagosPendientes?.find(
        (p) => p.saldoRestante > 0
    );

    // Usamos la data de "ultimoPago" si existe; de lo contrario, el pendiente activo
    const ultimoPago = ultimoPagoQuery.data || pagoPendienteActivo;

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

    return {
        deudas: deudasQuery.data,
        ultimoPago,
        isLoading: deudasQuery.isLoading || ultimoPagoQuery.isLoading,
        error: deudasQuery.error || ultimoPagoQuery.error,
    };
};
