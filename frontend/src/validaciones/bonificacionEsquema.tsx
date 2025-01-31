import * as Yup from "yup";

export const bonificacionEsquema = Yup.object().shape({
  descripcion: Yup.string()
    .min(3, "La descripción debe tener al menos 3 caracteres")
    .max(50, "La descripción no puede superar los 50 caracteres")
    .required("La descripción es obligatoria"),
  porcentajeDescuento: Yup.number()
    .min(1, "El porcentaje debe ser mayor a 0")
    .max(100, "El porcentaje no puede ser mayor a 100")
    .required("El porcentaje de descuento es obligatorio"),
  activo: Yup.boolean().required(),
  observaciones: Yup.string().max(
    255,
    "Las observaciones no pueden superar los 255 caracteres"
  ),
});
