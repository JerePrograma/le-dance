// hooks/useCargaMensualidades.ts
import { useCallback } from "react";
import matriculasApi from "../api/matriculasApi";
import reporteMensualidadApi from "../api/reporteMensualidadApi";
import { AlumnoListadoResponse, ReporteMensualidadDTO } from "../types/types";

const MATRICULA_FEE = 100000;

const formatearFecha = (d: Date): string =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
        d.getDate()
    ).padStart(2, "0")}`;

export const useCargaMensualidades = (alumnos: AlumnoListadoResponse[]) => {
    const isCuotaPendiente = (cuota: ReporteMensualidadDTO): boolean =>
        cuota.estado?.toUpperCase() === "PENDIENTE";

    const loadMensualidadesAlumno = useCallback(
        async (alumnoId: number): Promise<any[]> => {
            const currentDate = new Date();
            const inicioMes = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
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
            /**  mensualidadId: number;
              alumno: {
                id: number;
                nombre: string;
              }
              cuota: string;
              importe: number;
              bonificacion: {
                id: number;
                descripcion: string;
                porcentajeDescuento: number;
                valorFijo: number;
              }
              total: number;
              recargo: number;
              estado: string;
              disciplina: {
                id: number;
                nombre: string;
                valorCuota: number;
              } */
            const cuotasDetails = mensualidades
                .filter(isCuotaPendiente)
                .map((cuota) => {
                    const totalCalculado =
                        Number(cuota.importe) - Number(cuota.bonificacion) + Number(cuota.recargo);
                    return {
                        id: cuota.mensualidadId,
                        codigoConcepto: cuota.disciplina.nombre,
                        concepto: `${cuota.disciplina} - CUOTA - ${new Date().toLocaleString("default", {
                            month: "long",
                            year: "numeric",
                        })}`,
                        cuota: cuota.cuota,
                        valorBase: cuota.importe,
                        bonificacionId: cuota.bonificacion,
                        recargoId: cuota.recargo,
                        aFavor: 0,
                        importe: cuota.importe,
                        aCobrar: totalCalculado,
                        abono: totalCalculado,
                    };
                });

            const matriculaResponse = await matriculasApi.obtenerMatricula(alumnoId);
            let matriculaDetail: any[] = [];
            if (matriculaResponse && !matriculaResponse.pagada) {
                matriculaDetail = [
                    {
                        id: null,
                        codigoConcepto: "MATRICULA",
                        concepto: "Matrícula",
                        cuota: "1",
                        valorBase: MATRICULA_FEE,
                        bonificacionId: "",
                        recargoId: "",
                        aFavor: 0,
                        importe: MATRICULA_FEE,
                        aCobrar: MATRICULA_FEE,
                        abono: 0,
                    },
                ];
            }

            const nuevosDetalles = [...cuotasDetails, ...matriculaDetail];
            return nuevosDetalles;
        },
        [alumnos]
    );

    return { loadMensualidadesAlumno };
};
