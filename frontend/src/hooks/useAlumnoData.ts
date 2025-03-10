// src/hooks/useAlumnoData.ts
import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import deudasApi from "../api/deudasApi";
import pagosApi from "../api/pagosApi";
import { toast } from "react-toastify";
import type { DeudasPendientesResponse, PagoResponse } from "../types/types";

export const useAlumnoData = (alumnoId: number) => {
    const isAlumnoValid = alumnoId > 0;

    // Consulta de deudas pendientes
    const deudasQuery = useQuery<DeudasPendientesResponse, Error>({
        queryKey: ["deudas", alumnoId],
        queryFn: () => deudasApi.obtenerDeudasPendientes(alumnoId),
        enabled: isAlumnoValid,
    });

    // Consulta del último pago (según lógica existente en backend)
    const ultimoPagoQuery = useQuery<PagoResponse, Error>({
        queryKey: ["ultimoPago", alumnoId],
        queryFn: () => pagosApi.obtenerUltimoPagoPorAlumno(alumnoId),
        enabled: isAlumnoValid,
    });

    // Extraemos de la respuesta de deudas el pago pendiente activo (con saldoRestante > 0)
    const pagoPendienteActivo = useMemo(() => {
        return deudasQuery.data?.pagosPendientes?.find((p) => p.saldoRestante > 0);
    }, [deudasQuery.data]);

    // Usamos el último pago obtenido o, si no existe, el pago activo de la deuda
    const ultimoPago = ultimoPagoQuery.data || pagoPendienteActivo;

    // Manejo de errores
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
        ultimoPago, // Este valor se usará para actualizar el campo "id"
        isLoading: deudasQuery.isLoading || ultimoPagoQuery.isLoading,
        error: deudasQuery.error || ultimoPagoQuery.error,
    };
};
