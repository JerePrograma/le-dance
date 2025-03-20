import * as Yup from "yup";

export const inscripcionEsquema = Yup.object().shape({
  alumno: Yup.object().shape({
    id: Yup.number()
      .min(1, "Debe seleccionar un alumno")
      .required("El alumno es obligatorio"),
  }),
  disciplina: Yup.object().shape({
    id: Yup.number()
      .min(1, "Debe seleccionar una disciplina")
      .required("La disciplina es obligatoria"),
  }),
  bonificacionId: Yup.number().nullable(),
  fechaInscripcion: Yup.date()
    .max(new Date(), "La fecha de inscripción no puede ser futura")
    .required("La fecha de inscripción es obligatoria"),
  fechaBaja: Yup.date().nullable(),
  notas: Yup.string()
    .max(255, "Las notas no pueden superar los 255 caracteres")
    .nullable(),
});
