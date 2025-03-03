// src/validaciones/tipoStockEsquema.ts
import * as yup from "yup";

export const tipoStockEsquema = yup.object().shape({
    descripcion: yup.string().required("La descripcion es requerida"),
    activo: yup.boolean().required("El estado activo es requerido"),
});
