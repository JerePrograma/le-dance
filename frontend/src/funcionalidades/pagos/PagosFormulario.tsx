import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import pagosApi from "../../api/pagosApi";
import Boton from "../../componentes/comunes/Boton";
import React, { useState, useEffect } from "react";
import type { PagoRegistroRequest } from "../../types/types";

// Valores iniciales para el registro de pago
const initialPaymentValues: PagoRegistroRequest = {
    fecha: "",
    fechaVencimiento: "",
    monto: 0,
    inscripcionId: 0,
    metodoPagoId: 0,
    recargoAplicada: false,
    bonificacionAplicada: false,
    saldoRestante: 0,
    observaciones: ""
};

// Esquema de validación con Yup
const paymentSchema = Yup.object().shape({
    fecha: Yup.string().required("La fecha es obligatoria"),
    fechaVencimiento: Yup.string().required("La fecha de vencimiento es obligatoria"),
    monto: Yup.number().min(0, "El monto debe ser mayor o igual a 0").required("El monto es obligatorio"),
    inscripcionId: Yup.number().min(1, "Seleccione una inscripción válida").required("La inscripción es obligatoria"),
    metodoPagoId: Yup.number().min(1, "Seleccione un método de pago válido").required("El método de pago es obligatorio"),
    saldoRestante: Yup.number().min(0, "El saldo restante no puede ser negativo").required("El saldo restante es obligatorio"),
    observaciones: Yup.string()
});

const PaymentForm: React.FC = () => {
    const navigate = useNavigate();
    const [formValues, setFormValues] = useState<PagoRegistroRequest>(initialPaymentValues);

    const handleSubmit = async (values: PagoRegistroRequest) => {
        try {
            const response = await pagosApi.registrarPago(values);
            toast.success("Pago registrado correctamente");
            navigate("/pagos");
        } catch (error) {
            console.error("Error al registrar el pago:", error);
            toast.error("Error al registrar el pago");
        }
    };

    // Puedes cargar datos adicionales (por ejemplo, lista de inscripciones o métodos de pago) si lo requieres.

    return (
        <div className="page-container">
            <h1 className="page-title">Registrar Pago</h1>
            <Formik
                initialValues={formValues}
                validationSchema={paymentSchema}
                onSubmit={handleSubmit}
                enableReinitialize
            >
                {() => (
                    <Form className="formulario max-w-4xl mx-auto">
                        <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div className="mb-4">
                                <label htmlFor="fecha" className="auth-label">Fecha:</label>
                                <Field name="fecha" type="date" className="form-input" />
                                <ErrorMessage name="fecha" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4">
                                <label htmlFor="fechaVencimiento" className="auth-label">Fecha de Vencimiento:</label>
                                <Field name="fechaVencimiento" type="date" className="form-input" />
                                <ErrorMessage name="fechaVencimiento" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4">
                                <label htmlFor="monto" className="auth-label">Monto:</label>
                                <Field name="monto" type="number" step="0.01" className="form-input" />
                                <ErrorMessage name="monto" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4">
                                <label htmlFor="inscripcionId" className="auth-label">Inscripción ID:</label>
                                <Field name="inscripcionId" type="number" className="form-input" />
                                <ErrorMessage name="inscripcionId" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4">
                                <label htmlFor="metodoPagoId" className="auth-label">Método de Pago:</label>
                                <Field name="metodoPagoId" type="number" className="form-input" />
                                <ErrorMessage name="metodoPagoId" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4 flex items-center">
                                <Field type="checkbox" name="recargoAplicada" className="form-checkbox" />
                                <label htmlFor="recargoAplicada" className="ml-2 auth-label">Aplicar Recargo</label>
                            </div>

                            <div className="mb-4 flex items-center">
                                <Field type="checkbox" name="bonificacionAplicada" className="form-checkbox" />
                                <label htmlFor="bonificacionAplicada" className="ml-2 auth-label">Aplicar Bonificación</label>
                            </div>

                            <div className="mb-4">
                                <label htmlFor="saldoRestante" className="auth-label">Saldo Restante:</label>
                                <Field name="saldoRestante" type="number" step="0.01" className="form-input" />
                                <ErrorMessage name="saldoRestante" component="div" className="auth-error" />
                            </div>

                            <div className="mb-4">
                                <label htmlFor="observaciones" className="auth-label">Observaciones:</label>
                                <Field as="textarea" name="observaciones" className="form-input" />
                                <ErrorMessage name="observaciones" component="div" className="auth-error" />
                            </div>
                        </div>
                        <div className="form-acciones">
                            <Boton type="submit" className="page-button">Registrar Pago</Boton>
                            <Boton type="button" onClick={() => navigate("/pagos")} className="page-button-secondary">
                                Cancelar
                            </Boton>
                        </div>
                    </Form>
                )}
            </Formik>
        </div>
    );
};

export default PaymentForm;
