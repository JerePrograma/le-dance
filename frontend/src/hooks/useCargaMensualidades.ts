import { useCallback } from "react";
import reporteMensualidadApi from "../api/reporteMensualidadApi";
import { AlumnoListadoResponse, ReporteMensualidadDTO } from "../types/types";

const formatearFecha = (d: Date): string =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
        d.getDate()
    ).padStart(2, "0")}`;

export const useCargaMensualidades = (alumnos: AlumnoListadoResponse[]) => {
    const loadMensualidadesAlumno = useCallback(
        async (alumnoId: number): Promise<ReporteMensualidadDTO[]> => {
            const currentDate = new Date();
            const inicioMes = new Date(
                currentDate.getFullYear(),
                currentDate.getMonth(),
                1
            );
            const fechaInicio = formatearFecha(inicioMes);

            const alumnoSeleccionado = alumnos.find((a) => a.id === alumnoId);
            if (!alumnoSeleccionado) {
                console.error("No se encontró el alumno seleccionado");
                return [];
            }
            const alumnoNombre = `${alumnoSeleccionado.nombre} ${alumnoSeleccionado.apellido}`;
            console.log("Buscando mensualidades para:", { fechaInicio, alumnoNombre });

            const mensualidades: ReporteMensualidadDTO[] =
                await reporteMensualidadApi.buscarMensualidadesAlumno({
                    fechaInicio,
                    alumnoNombre,
                });
            // Filtrar solo las cuotas pendientes
            const pendientes = mensualidades.filter(
                (cuota) => cuota.estado?.toUpperCase() === "PENDIENTE"
            );
            return pendientes;
        },
        [alumnos]
    );

    return { loadMensualidadesAlumno };
};
