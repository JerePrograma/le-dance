import * as Yup from "yup";

export const asistenciaMensualEsquema = Yup.object().shape({
  mes: Yup.number()
    .min(1, "El mes debe ser un número entre 1 y 12")
    .max(12, "El mes debe ser un número entre 1 y 12")
    .integer("El mes debe ser un número entero")
    .required("El mes es requerido"),

  anio: Yup.number()
    .min(2000, "El año debe ser 2000 o posterior")
    .max(new Date().getFullYear() + 1, "El año no puede ser futuro")
    .integer("El año debe ser un número entero")
    .required("El año es requerido"),

  inscripcionId: Yup.number()
    .positive("El ID de inscripción debe ser un número positivo")
    .integer("El ID de inscripción debe ser un número entero")
    .required("El ID de inscripción es requerido"),
});
