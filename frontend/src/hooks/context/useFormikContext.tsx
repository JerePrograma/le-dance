import { useEffect } from "react";
import { useFormikContext } from "formik";
import { CobranzasFormValues, ConceptoResponse, MatriculaResponse } from "../../types/types";

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
                console.error("No se encontró el concepto de matrícula en la lista.");
                return;
            }
            if (values.matriculaRemoved) return;
            // Comparamos por descripción, por ejemplo, si algún detalle tiene concepto igual a "matricula"
            const hasMatriculaDetail = values.detalles.some(
                (detail) => detail.concepto?.toLowerCase().trim() === "matricula"
            );
            if (!matricula.pagada && !hasMatriculaDetail) {
                const matriculaDetail = {
                    codigoConcepto: conceptoMatricula.id.toString(),
                    concepto: conceptoMatricula.descripcion, // debe ser "Matricula" según lo definido
                    cuota: "1",
                    valorBase: conceptoMatricula.precio,
                    bonificacion: 0,
                    recargo: 0,
                    aFavor: 0,
                    importe: conceptoMatricula.precio,
                    aCobrar: conceptoMatricula.precio,
                };
                setFieldValue("detalles", [...values.detalles, matriculaDetail]);
            } else if (matricula.pagada && hasMatriculaDetail) {
                const nuevosDetalles = values.detalles.filter(
                    (detail) =>
                        detail.concepto?.toLowerCase().trim() !== "matricula"
                );
                setFieldValue("detalles", nuevosDetalles);
                setFieldValue("matriculaRemoved", false);
            }
        }
    }, [values.alumno, values.detalles, matricula, setFieldValue, conceptos]);

    return null;
};
