// Esquema de validacion con Yup
import * as Yup from "yup";

export const pagoEsquema = Yup.object().shape({
    fecha: Yup.string().required("La fecha es obligatoria"),
    fechaVencimiento: Yup.string().required("La fecha de vencimiento es obligatoria"),
    monto: Yup.number().min(0, "El monto debe ser mayor o igual a 0").required("El monto es obligatorio"),
    inscripcionId: Yup.number().min(1, "Seleccione una inscripcion valida").required("La inscripcion es obligatoria"),
    metodoPagoId: Yup.number().min(1, "Seleccione un metodo de pago valido").required("El metodo de pago es obligatorio"),
    saldoRestante: Yup.number().min(0, "El saldo restante no puede ser negativo").required("El saldo restante es obligatorio"),
    observaciones: Yup.string()
});