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
      // Buscar el concepto de matrícula (compara la descripción en minúsculas)
      const conceptoMatricula = conceptos.find(
        (c) =>
          c.descripcion.toLowerCase() === "matricula" ||
          c.descripcion.toLowerCase().includes("matricula")
      );
      if (!conceptoMatricula) {
        toast.error("No se encontró el concepto de matrícula en la lista.");
        return;
      }
      if (values.matriculaRemoved) return;

      // Verificar si ya existe un detalle de matrícula usando el id del concepto
      const hasMatriculaDetail = values.detallePagos.some(
        (detail) => detail.conceptoId === conceptoMatricula.id
      );

      // Si la matrícula NO está pagada y no existe el detalle, se agrega
      if (!matricula.pagada && !hasMatriculaDetail) {
        const matriculaDetail = {
          autoGenerated: true,
          // id se asigna por el backend o se deja indefinido
          version: 0,
          descripcionConcepto: conceptoMatricula.descripcion.toUpperCase(), // "MATRICULA"
          cuotaOCantidad: "1",
          valorBase: conceptoMatricula.precio,
          aCobrar: conceptoMatricula.precio,
          bonificacionId: null,
          recargoId: null,
          cobrado: false,
          conceptoId: conceptoMatricula.id,
          subConceptoId: null,
          importePendiente: conceptoMatricula.precio,
          mensualidadId: null,
          matriculaId: null,
          stockId: null,
          pagoId: null,
        };
        setFieldValue("detallePagos", [...values.detallePagos, matriculaDetail]);
      } 
      // Si la matrícula está pagada y existe el detalle, se remueve
      else if (matricula.pagada && hasMatriculaDetail) {
        const nuevosDetalles = values.detallePagos.filter(
          (detail) => detail.conceptoId !== conceptoMatricula.id
        );
        setFieldValue("detallePagos", nuevosDetalles);
        setFieldValue("matriculaRemoved", false);
      }
    }
  }, [values.alumno, values.detallePagos, matricula, setFieldValue, conceptos]);

  return null;
};
