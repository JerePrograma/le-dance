// src/components/PaymentIdUpdater.tsx
import { useEffect } from "react";
import { useFormikContext } from "formik";
import { useSearchParams } from "react-router-dom";
import type { CobranzasFormValues, PagoResponse } from "../types/types";

interface PaymentIdUpdaterProps {
    ultimoPago?: PagoResponse;
}

export const PaymentIdUpdater: React.FC<PaymentIdUpdaterProps> = ({ ultimoPago }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
    const [searchParams, setSearchParams] = useSearchParams();

    // Actualiza el campo "id" del formulario
    useEffect(() => {
        if (ultimoPago && values.id !== ultimoPago.id) {
            setFieldValue("id", ultimoPago.id);
        }
    }, [ultimoPago, values.id, setFieldValue]);

    // Actualiza el query param "id" en la URL
    useEffect(() => {
        if (ultimoPago) {
            const currentQueryId = searchParams.get("id");
            const nuevoId = ultimoPago.id.toString();
            if (currentQueryId !== nuevoId) {
                setSearchParams({ id: nuevoId }, { replace: true });
            }
        }
    }, [ultimoPago, searchParams, setSearchParams]);

    return null;
};
