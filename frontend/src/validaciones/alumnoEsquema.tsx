import * as Yup from "yup";

export const alumnoEsquema = Yup.object().shape({
  nombre: Yup.string()
    .min(2, "El nombre debe tener al menos 2 caracteres")
    .required("El nombre es obligatorio"),
  apellido: Yup.string().required("El apellido es obligatorio"),
  fechaNacimiento: Yup.date().required("La fecha de nacimiento es obligatoria"),
  celular1: Yup.string()
    .matches(/^\d{10}$/, "Debe ser un numero de 10 digitos")
    .required("El celular es obligatorio"),
  email1: Yup.string()
    .email("Debe ser un email valido")
    .required("El email es obligatorio"),
  documento: Yup.string()
    .matches(/^\d+$/, "Debe contener solo numeros")
    .required("El documento es obligatorio"),
});
