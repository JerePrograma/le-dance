import * as Yup from "yup";

export const alumnoEsquema = Yup.object().shape({
  nombre: Yup.string()
    .min(2, "El nombre debe tener al menos 2 caracteres")
    .required("El nombre es obligatorio"),
  apellido: Yup.string().required("El apellido es obligatorio"),
  fechaNacimiento: Yup.date().required("La fecha de nacimiento es obligatoria"),
});
