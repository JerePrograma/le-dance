import * as Yup from "yup";

export const disciplinaEsquema = Yup.object().shape({
  nombre: Yup.string()
    .min(1, "El nombre debe tener al menos 3 caracteres")
    .required("El nombre es obligatorio"),
  duracion: Yup.string().required("La duracion es obligatoria"),
  salon: Yup.string().required("El salon es obligatorio"),
  valorCuota: Yup.number(),
  profesorId: Yup.number()
    .min(1, "Debe seleccionar un profesor")
    .required("El profesor es obligatorio")
});
