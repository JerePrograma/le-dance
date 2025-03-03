import * as Yup from "yup";

export const rolEsquema = Yup.object().shape({
  descripcion: Yup.string()
    .min(3, "La descripcion debe tener al menos 3 caracteres")
    .required("La descripcion es obligatoria"),
});
