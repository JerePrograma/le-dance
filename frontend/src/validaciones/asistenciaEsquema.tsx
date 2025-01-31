import * as Yup from "yup";

export const asistenciaEsquema = Yup.object().shape({
  fecha: Yup.date().required("La fecha es obligatoria"),
  alumnoId: Yup.number()
    .min(1, "Debe seleccionar un alumno")
    .required("El alumno es obligatorio"),
  disciplinaId: Yup.number()
    .min(1, "Debe seleccionar una disciplina")
    .required("La disciplina es obligatoria"),
  presente: Yup.boolean().required(),
  observacion: Yup.string().max(
    255,
    "Las observaciones no pueden superar los 255 caracteres"
  ),
});
