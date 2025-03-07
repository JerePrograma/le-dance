import { useEffect } from "react";
import { useFormikContext } from "formik";
import { CobranzasFormValues, ConceptoResponse, MatriculaResponse } from "../../types/types";
import { toast } from "react-toastify";

interface MatriculaAutoAddProps {
    matricula: MatriculaResponse | null;
    conceptos: ConceptoResponse[];
}

export const MatriculaAutoAdd: React.FC<MatriculaAutoAddProps> = ({ matricula, conceptos }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

    useEffect(() => {
        if (values.alumno && matricula && conceptos.length > 0) {
            const conceptoMatricula = conceptos.find(
                (c) =>
                    c.descripcion.toLowerCase() === "matricula" ||
                    c.descripcion.toLowerCase().includes("matricula")
            );
            if (!conceptoMatricula) {
                toast.error("No se encontro el concepto de matricula en la lista.");
                return;
            }
            if (values.matriculaRemoved) return;

            // Cambiamos "detalles" por "detallePagos"
            const hasMatriculaDetail = values.detallePagos.some(
                (detail) => detail.concepto?.toLowerCase().trim() === "matricula"
            );

            if (!matricula.pagada && !hasMatriculaDetail) {
                const matriculaDetail = {
                    codigoConcepto: conceptoMatricula.id.toString(),
                    concepto: conceptoMatricula.descripcion, // debe ser "Matricula" segun lo definido
                    cuota: "1",
                    valorBase: conceptoMatricula.precio,
                    bonificacionId: undefined,
                    recargoId: undefined,
                    aFavor: 0,
                    importe: conceptoMatricula.precio,
                    aCobrar: conceptoMatricula.precio,
                    abono: 0,
                };
                setFieldValue("detallePagos", [...values.detallePagos, matriculaDetail]);
            } else if (matricula.pagada && hasMatriculaDetail) {
                const nuevosDetalles = values.detallePagos.filter(
                    (detail) => detail.concepto?.toLowerCase().trim() !== "matricula"
                );
                setFieldValue("detallePagos", nuevosDetalles);
                setFieldValue("matriculaRemoved", false);
            }
        }
    }, [values.alumno, values.detallePagos, matricula, setFieldValue, conceptos]);

    return null;
};
