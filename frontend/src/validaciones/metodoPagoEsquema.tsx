// src/validaciones/metodoPagoEsquema.ts
import * as Yup from "yup";

export const metodoPagoEsquema = Yup.object().shape({
    descripcion: Yup.string()
        .trim()
        .required("La descripcion es obligatoria"),
});
