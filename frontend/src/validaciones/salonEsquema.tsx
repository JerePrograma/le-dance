import * as Yup from "yup";

export const salonEsquema = Yup.object().shape({
  nombre: Yup.string()
    .required("El nombre es obligatorio")
    .min(2, "El nombre debe tener al menos 2 caracteres"),
  descripcion: Yup.string().nullable(),
});
