import * as Yup from "yup";

export const inscripcionEsquema = Yup.object().shape({
  alumnoId: Yup.number()
    .min(1, "Debe seleccionar un alumno")
    .required("El alumno es obligatorio"),
  disciplinaId: Yup.number()
    .min(1, "Debe seleccionar una disciplina")
    .required("La disciplina es obligatoria"),
  bonificacionId: Yup.number().nullable(),
  costoParticular: Yup.number()
    .min(0, "El costo debe ser mayor o igual a 0")
    .required("El costo es obligatorio"),
  notas: Yup.string()
    .max(255, "Las notas no pueden superar los 255 caracteres")
    .nullable(),
});
