import * as Yup from "yup";

export const bonificacionEsquema = Yup.object().shape({
  descripcion: Yup.string()
    .min(3, "La descripcion debe tener al menos 3 caracteres")
    .max(50, "La descripcion no puede superar los 50 caracteres")
    .required("La descripcion es obligatoria"),
  porcentajeDescuento: Yup.number()
    .max(100, "El porcentaje no puede ser mayor a 100"),
  activo: Yup.boolean().required(),
  observaciones: Yup.string().max(
    255,
    "Las observaciones no pueden superar los 255 caracteres"
  ),
});

