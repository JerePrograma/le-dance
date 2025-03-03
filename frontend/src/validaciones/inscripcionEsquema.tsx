import * as Yup from "yup";

export const inscripcionEsquema = Yup.object().shape({
  alumnoId: Yup.number()
    .min(1, "Debe seleccionar un alumno")
    .required("El alumno es obligatorio"),
  inscripcion: Yup.object().shape({
    disciplinaId: Yup.number()
      .min(1, "Debe seleccionar una disciplina")
      .required("La disciplina es obligatoria"),
    bonificacionId: Yup.number().nullable(),
  }),
  fechaInscripcion: Yup.date().required(
    "La fecha de inscripcion es obligatoria"
  ),
  fechaBaja: Yup.date().nullable(),
  notas: Yup.string()
    .max(255, "Las notas no pueden superar los 255 caracteres")
    .nullable(),
});
