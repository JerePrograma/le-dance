import { useEffect, useRef } from "react";
import { useFormikContext } from "formik";
import {
    CobranzasFormValues,
    ReporteMensualidadDTO,
    ConceptoResponse,
    MatriculaResponse,
} from "../../types/types";

interface AutoAddDetallesProps {
    matricula: MatriculaResponse | null;
    mensualidades: ReporteMensualidadDTO[];
    conceptos: ConceptoResponse[];
}

export const AutoAddDetalles: React.FC<AutoAddDetallesProps> = ({
    matricula,
    mensualidades,
    conceptos,
}) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
    const lastAlumnoRef = useRef<string | null>(null);

    useEffect(() => {
        if (!values.alumno) return;

        if (lastAlumnoRef.current !== values.alumno) {
            lastAlumnoRef.current = values.alumno;
            // Aquí podrías reiniciar detalles manuales si lo deseas
        }

        // --- Construir detalles auto-generados utilizando los datos del backend ---
        const computedAutoDetails = mensualidades.map((cuota) => {
            const disciplinaId = cuota?.disciplina?.id ?? 0;
            const descripcion = cuota?.descripcion ?? "Sin Disciplina";
            const cantidad = 1;
            // Usamos el valor de cuota de la disciplina como valorBase;
            // si no viene, se toma el "importe" como fallback.
            const valorBase = cuota?.disciplina?.valorCuota || Number(cuota.importe) || 0;
            // Para "importe" y "aCobrar" usamos el valor "total" del backend.
            const total = Number(cuota.total) || Number(cuota.importe);
            // Para la bonificación, usamos el valor fijo (que representa el descuento)
            const montoBonificacion = cuota?.bonificacion ? Number(cuota.bonificacion.valorFijo) : 0;
            // Recargo, si existe.
            const recargo = Number(cuota.recargo) || 0;

            return {
                autoGenerated: true,
                id: cuota.mensualidadId,
                codigoConcepto: disciplinaId,
                concepto: descripcion,
                cuota: cantidad.toString(),
                valorBase: valorBase,
                // Asignamos el monto de bonificación calculado (valorFijo) en un nuevo campo
                montoBonificacion: montoBonificacion,
                recargoId: recargo ? recargo.toString() : "",
                aFavor: 0,
                importe: total,
                aCobrar: total,
                abono: 0,
            };
        });

        let computedMatriculaDetails: any[] = [];
        if (matricula && !matricula.pagada) {
            const conceptoMatricula = conceptos.find((c) =>
                c.descripcion.toLowerCase().includes("matricula")
            );
            const precio = conceptoMatricula ? conceptoMatricula.precio : 0;
            computedMatriculaDetails.push({
                autoGenerated: true,
                id: null, // Puede ser null o un identificador especial
                codigoConcepto: conceptoMatricula ? Number(conceptoMatricula.id) : 0,
                concepto: "Matrícula",
                cuota: "1",
                valorBase: precio,
                montoBonificacion: 0,
                recargoId: "",
                aFavor: 0,
                importe: precio,
                aCobrar: precio,
                abono: 0,
            });
        }

        const newAutoDetails = [...computedAutoDetails, ...computedMatriculaDetails];

        // Actualizamos los detalles auto-generados sin alterar el orden de los manuales
        const updatedDetails = values.detallePagos.map((detalle) => {
            if (detalle.autoGenerated) {
                const newVersion = newAutoDetails.find((nd) => nd.id === detalle.id);
                return newVersion ? newVersion : detalle;
            }
            return detalle;
        });
        newAutoDetails.forEach((autoDetail) => {
            const exists = values.detallePagos.some((detalle) => detalle.id === autoDetail.id);
            if (!exists) {
                updatedDetails.push(autoDetail);
            }
        });

        if (JSON.stringify(updatedDetails) !== JSON.stringify(values.detallePagos)) {
            setFieldValue("detallePagos", updatedDetails);
        }
    }, [
        values.alumno,
        mensualidades,
        matricula,
        conceptos,
        setFieldValue,
        values.detallePagos,
    ]);

    return null;
};
