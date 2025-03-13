import * as yup from "yup";

export const stockEsquema = yup.object().shape({
    nombre: yup.string().required("El nombre es requerido"),
    precio: yup
        .number()
        .typeError("El precio debe ser un numero")
        .positive("El precio debe ser mayor que 0")
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
