import * as Yup from "yup";

export const profesorEsquema = Yup.object().shape({
  nombre: Yup.string()
    .min(2, "El nombre debe tener al menos 2 caracteres")
    .required("El nombre es obligatorio"),
  apellido: Yup.string()
    .min(2, "El apellido debe tener al menos 2 caracteres")
    .required("El apellido es obligatorio"),
  especialidad: Yup.string()
    .min(3, "Debe especificar una especialidad válida")
    .required("La especialidad es obligatoria"),
  aniosExperiencia: Yup.number()
    .min(0, "Debe ser un número positivo")
    .max(50, "No puede ser mayor a 50 años")
    .required("Los años de experiencia son obligatorios"),
});
