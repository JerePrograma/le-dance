// src/components/PaymentIdUpdater.tsx
import { useEffect, useRef } from "react";
import { useFormikContext } from "formik";
import { useSearchParams } from "react-router-dom";
import type { CobranzasFormValues, PagoResponse } from "../types/types";

interface PaymentIdUpdaterProps {
    ultimoPago?: PagoResponse;
}

export const PaymentIdUpdater: React.FC<PaymentIdUpdaterProps> = ({ ultimoPago }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
    const [searchParams, setSearchParams] = useSearchParams();
    const currentQueryId = searchParams.get("id");
    const prevFormIdRef = useRef<number | null>(null);

    useEffect(() => {
        if (ultimoPago && values.id !== ultimoPago.id && ultimoPago.id !== prevFormIdRef.current) {
            setFieldValue("id", ultimoPago.id);
            prevFormIdRef.current = ultimoPago.id;
            // Actualizamos solo si el query param no coincide ya con el valor
            if (currentQueryId !== ultimoPago.id.toString()) {
                setSearchParams({ id: ultimoPago.id.toString() }, { replace: true });
            }
        }
    }, [ultimoPago, values.id, setFieldValue, currentQueryId, setSearchParams]);
    return null;
};
