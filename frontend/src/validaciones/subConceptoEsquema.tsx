import * as yup from "yup";

export const subConceptoEsquema = yup.object({
    descripcion: yup
        .string()
        .required("La descripción es obligatoria")
        .trim(),
});
