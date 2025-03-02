// Esquema de validación con Yup
import * as Yup from "yup";

export const recargoEsquema = Yup.object().shape({
    descripcion: Yup.string()
        .trim()
        .min(3, "Debe tener al menos 3 caracteres")
        .max(100, "Máximo 100 caracteres")
        .required("La descripción es obligatoria"),
});