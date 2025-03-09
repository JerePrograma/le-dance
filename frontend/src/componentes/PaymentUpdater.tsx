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

    useEffect(() => {
        console.log("[PaymentIdUpdater] Efecto ejecutado. UltimoPago:", ultimoPago);
        console.log("[PaymentIdUpdater] Valores actuales del form:", values);
        if (ultimoPago) {
            if (values.id !== ultimoPago.id) {
                console.log("[PaymentIdUpdater] Actualizando field 'id' a:", ultimoPago.id);
                setFieldValue("id", ultimoPago.id);
            }
            if (searchParams.get("id") !== ultimoPago.id.toString()) {
                console.log("[PaymentIdUpdater] Actualizando query param 'id' a:", ultimoPago.id);
                setSearchParams({ id: ultimoPago.id.toString() }, { replace: true });
            }
        } else {
            console.log("[PaymentIdUpdater] UltimoPago no está definido");
        }
    }, [ultimoPago, values.id, setFieldValue, searchParams, setSearchParams]);

    return null;
};
