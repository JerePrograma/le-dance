import * as yup from "yup";
import { isPositiveMoney } from "../utils/money";

export const stockEsquema = yup.object().shape({
    nombre: yup.string().required("El nombre es requerido"),
    precio: yup
        .string()
        .test("money", "El precio debe ser mayor que 0 y tener hasta dos decimales", (value) => !value || isPositiveMoney(value))
        .required("El precio es requerido"),
    stock: yup
        .number()
        .typeError("El stock debe ser un numero")
        .min(0, "El stock no puede ser negativo")
        .required("El stock es requerido"),
    requiereControlDeStock: yup
        .boolean()
        .required("Debe especificar si requiere control de stock"),
    codigoBarras: yup.string().nullable(),
    activo: yup.boolean().required("El estado activo es requerido"),
});
