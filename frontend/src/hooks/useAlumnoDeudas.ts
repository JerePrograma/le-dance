// useAlumnoDeudas.ts
import { useCallback } from "react";
import pagosApi from "../api/pagosApi";
import { toast } from "react-toastify";

export const useAlumnoDeudas = () => {
  const loadDeudasForAlumno = useCallback(
    (alumnoId: number, setFieldValue: any) => {
      pagosApi
        .obtenerCobranzaPorAlumno(alumnoId)
        .then((cobranza) => {
          if (cobranza.detalles && cobranza.detalles.length > 0) {
            const deudas = cobranza.detalles.map((det: any) => ({
              id: null, // Se crean como nuevos detalles
              codigoConcepto: "",
              concepto: det.descripcion,
              cuota: "1",
              valorBase: det.monto,
              bonificacionId: "",
              recargoId: "",
              aFavor: 0,
              importe: det.monto,
              aCobrar: det.monto,
              abono: 0,
            }));
            setFieldValue("detallePagos", deudas);
          } else {
            setFieldValue("detallePagos", []);
          }
        })
        .catch(toast.error);
    },
    []
  );

  return { loadDeudasForAlumno };
};
