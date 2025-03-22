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
  const currentQueryId = searchParams.get("id");

  useEffect(() => {
    if (ultimoPago && values.id !== ultimoPago.id) {
      setFieldValue("id", ultimoPago.id);
      if (currentQueryId !== ultimoPago.id.toString()) {
        setSearchParams({ id: ultimoPago.id.toString() }, { replace: true });
      }
    }
  }, [ultimoPago, values.id, currentQueryId, setFieldValue, setSearchParams]);

  return null;
};
