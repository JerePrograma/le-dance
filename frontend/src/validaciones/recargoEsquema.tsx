// Esquema de validacion con Yup
import * as Yup from "yup";

export const recargoEsquema = Yup.object().shape({
    descripcion: Yup.string()
        .trim()
        .min(3, "Debe tener al menos 3 caracteres")
        .max(100, "Maximo 100 caracteres")
        .required("La descripcion es obligatoria"),
});