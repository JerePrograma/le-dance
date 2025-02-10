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
  fechaNacimiento: Yup.date()
    .nullable()
    .max(new Date(), "La fecha de nacimiento no puede ser en el futuro"),
  telefono: Yup.string().nullable(),
  activo: Yup.boolean(),
});
