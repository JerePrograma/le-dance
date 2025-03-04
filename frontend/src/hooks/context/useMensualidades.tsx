// hooks/context/useMensualidades.tsx
import { useEffect } from "react";
import { useFormikContext } from "formik";
import type { CobranzasFormValues, ReporteMensualidadDTO } from "../../types/types";

interface MensualidadesAutoAddProps {
    mensualidades: ReporteMensualidadDTO[];
}

export const MensualidadesAutoAdd: React.FC<MensualidadesAutoAddProps> = ({ mensualidades }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

    useEffect(() => {
        if (values.alumno && Array.isArray(mensualidades) && mensualidades.length > 0) {
            // Mapear cada cuota para generar el detalle de pago
            const nuevosDetalles = mensualidades.map((cuota) => {
                console.log("Mapping cuota:", cuota);
                // Extraemos correctamente el id y el nombre de la disciplina
                const disciplinaId =
                    cuota?.disciplina?.id != null ? cuota.disciplina.id : 0;
                const disciplinaNombre =
                    cuota?.disciplina?.nombre != null ? String(cuota.disciplina.nombre) : "Sin Disciplina";

                const cantidad = 1; // Valor por defecto (puedes modificarlo si es necesario)
                const cuotaImporte =
                    cuota?.importe != null && !isNaN(Number(cuota.importe))
                        ? Number(cuota.importe)
                        : 0;
                // Para la bonificación, si el objeto existe usamos su valorFijo (ajusta si requieres otra lógica)
                const cuotaBonificacion =
                    cuota?.bonificacion != null && !isNaN(Number(cuota.bonificacion.valorFijo))
                        ? Number(cuota.bonificacion.valorFijo)
                        : 0;
                const cuotaRecargo =
                    cuota?.recargo != null && !isNaN(Number(cuota.recargo))
                        ? Number(cuota.recargo)
                        : 0;
                const cuotaTotal = cuotaImporte - cuotaBonificacion + cuotaRecargo;

                return {
                    // Asignamos el id de la disciplina a "codigoConcepto"
                    codigoConcepto: disciplinaId,
                    // Asignamos el nombre de la disciplina a "concepto"
                    concepto: disciplinaNombre,
                    // La cantidad se guarda en "cuota" como string
                    cuota: cantidad.toString(),
                    // Asignamos el importe a "valorBase" e "importe"
                    valorBase: cuotaImporte,
                    importe: cuotaImporte,
                    // Convertimos bonificación y recargo a string (o cadena vacía si 0)
                    bonificacionId: cuotaBonificacion ? cuotaBonificacion.toString() : "",
                    recargoId: cuotaRecargo ? cuotaRecargo.toString() : "",
                    aFavor: 0,
                    aCobrar: cuotaTotal,
                    abono: 0,
                };
            });
            setFieldValue("detallePagos", nuevosDetalles);
        }
    }, [values.alumno, mensualidades, setFieldValue]);

    return null;
};
