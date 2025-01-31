import * as Yup from "yup";

export const disciplinaEsquema = Yup.object().shape({
  nombre: Yup.string()
    .min(3, "El nombre debe tener al menos 3 caracteres")
    .required("El nombre es obligatorio"),
  horario: Yup.string().required("El horario es obligatorio"),
  frecuenciaSemanal: Yup.number()
    .min(1, "Debe ser al menos 1 vez por semana")
    .required("La frecuencia es obligatoria"),
  duracion: Yup.string().required("La duración es obligatoria"),
  salon: Yup.string().required("El salón es obligatorio"),
  valorCuota: Yup.number()
    .min(0, "El valor de la cuota no puede ser negativo")
    .required("El valor de la cuota es obligatorio"),
  matricula: Yup.number()
    .min(0, "La matrícula no puede ser negativa")
    .required("La matrícula es obligatoria"),
  profesorId: Yup.number()
    .min(1, "Debe seleccionar un profesor")
    .required("El profesor es obligatorio"),
  cupoMaximo: Yup.number()
    .min(1, "Debe ser al menos 1")
    .required("El cupo máximo es obligatorio"),
});
