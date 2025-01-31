import * as Yup from "yup";

export const rolEsquema = Yup.object().shape({
  descripcion: Yup.string()
    .min(3, "La descripción debe tener al menos 3 caracteres")
    .required("La descripción es obligatoria"),
});
