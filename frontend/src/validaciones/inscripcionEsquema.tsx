import * as Yup from "yup";

export const inscripcionEsquema = Yup.object().shape({
  alumnoId: Yup.number()
    .min(1, "Debe seleccionar un alumno")
    .required("El alumno es obligatorio"),
  // Ahora validamos la disciplina como un objeto
  disciplina: Yup.object().shape({
    id: Yup.number()
      .min(1, "Debe seleccionar una disciplina")
      .required("La disciplina es obligatoria"),
    // Puedes agregar validaciones para otros campos de disciplina si fuera necesario
  }),
  bonificacionId: Yup.number().nullable(),
  fechaInscripcion: Yup.date().required("La fecha de inscripción es obligatoria"),
  // Si usas estos campos, puedes mantenerlos:
  fechaBaja: Yup.date().nullable(),
  notas: Yup.string()
    .max(255, "Las notas no pueden superar los 255 caracteres")
    .nullable(),
});
