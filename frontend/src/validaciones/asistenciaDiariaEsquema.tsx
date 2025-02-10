import * as Yup from "yup";
import { EstadoAsistencia } from "../types/types";

export const asistenciaDiariaEsquema = Yup.object().shape({
  fecha: Yup.date()
    .required("La fecha es requerida")
    .max(new Date(), "La fecha no puede ser futura"),

  estado: Yup.mixed<EstadoAsistencia>()
    .oneOf(Object.values(EstadoAsistencia), "Estado de asistencia inválido")
    .required("El estado de asistencia es requerido"),

  alumnoId: Yup.number()
    .positive("El ID del alumno debe ser un número positivo")
    .integer("El ID del alumno debe ser un número entero")
    .required("El ID del alumno es requerido"),

  asistenciaMensualId: Yup.number()
    .positive("El ID de asistencia mensual debe ser un número positivo")
    .integer("El ID de asistencia mensual debe ser un número entero")
    .required("El ID de asistencia mensual es requerido"),
});
