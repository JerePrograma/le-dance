// src/forms/CobranzasForm.tsx
import React, { useState, useCallback, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { Formik, Form, Field, FieldArray, useFormikContext, FormikErrors } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import matriculasApi from "../../api/matriculasApi";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import { useAlumnoData } from "../../hooks/useAlumnoData";
import { useInscripcionesActivas } from "../../hooks/useInscripcionesActivas";
import type {
    StockResponse,
    PagoRegistroRequest,
    ConceptoResponse,
    MatriculaResponse,
    CobranzasFormValues,
    MetodoPagoResponse,
    DisciplinaDetalleResponse,
    InscripcionResponse,
} from "../../types/types";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";
import { AutoAddDetalles } from "../../hooks/context/AutoAddDetalles";
import FormHeader from "../../componentes/FormHeader";
import { PaymentIdUpdater } from "../../componentes/PaymentUpdater";

// --- Utilidades ---
const getMesVigente = () =>
    new Date()
        .toLocaleString("default", {
            month: "long",
            year: "numeric",
            timeZone: "America/Argentina/Buenos_Aires",
        })
        .toUpperCase();

// Valores iniciales para el formulario
const defaultValues: CobranzasFormValues = {
    id: 0,
    reciboNro: "AUTO-001",
    alumno: "",
    alumnoId: "",
    inscripcionId: "",
    fecha: new Date().toISOString().split("T")[0],
    detallePagos: [],
    disciplina: "",
    tarifa: "",
    conceptoSeleccionado: "",
    stockSeleccionado: "",
    cantidad: 1,
    totalCobrado: 0,
    totalACobrar: 0,
    metodoPagoId: "",
    observaciones: "",
    matriculaRemoved: false,
    mensualidadId: "",
    periodoMensual: getMesVigente(),
    autoRemoved: [] // <-- Nuevo campo
};

const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria"),
});

// --- Componentes internos ---
const FormValuesUpdater: React.FC<{ newValues: Partial<CobranzasFormValues> }> = ({ newValues }) => {
    const { values, setValues } = useFormikContext<CobranzasFormValues>();
    useEffect(() => {
        setValues({ ...values, ...newValues });
    }, [newValues, setValues]);
    return null;
};

const TotalsUpdater: React.FC<{ metodosPago: MetodoPagoResponse[] }> = ({ metodosPago }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();
    useEffect(() => {
        const baseTotal = values.detallePagos.reduce(
            (total, item) => total + Number(item.importe || 0),
            0
        );
        let recargoValue = 0;
        if (values.metodoPagoId) {
            const selectedMetodo = metodosPago.find(
                (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
            );
            if (selectedMetodo && selectedMetodo.descripcion.toUpperCase() === "DEBITO") {
                recargoValue = Number(selectedMetodo.recargo) || 0;
            }
        }
        const newTotalACobrar = baseTotal + recargoValue;
        const newTotalCobrado = values.detallePagos.reduce(
            (total, item) => total + Number(item.aCobrar || 0),
            0
        );
        setFieldValue("totalACobrar", newTotalACobrar);
        setFieldValue("totalCobrado", newTotalCobrado);
    }, [values.detallePagos, values.metodoPagoId, metodosPago, setFieldValue]);
    return null;
};

const ConceptosSection: React.FC<{
    inscripcionesData: InscripcionResponse[] | undefined;
    disciplinas: DisciplinaDetalleResponse[];
    stocks: StockResponse[];
    conceptos: ConceptoResponse[];
    generatePeriodos: (numMeses?: number) => string[];
    values: CobranzasFormValues;
    setFieldValue: any;
    handleAgregarDetalle: (values: CobranzasFormValues, setFieldValue: any) => void;
}> = ({
    inscripcionesData,
    disciplinas,
    stocks,
    conceptos,
    generatePeriodos,
    values,
    setFieldValue,
    handleAgregarDetalle,
}) => (
        <div className="border p-4 mb-4">
            <h2 className="font-bold mb-2">Datos de Disciplina y Conceptos</h2>
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
                <div className="sm:col-span-2">
                    <label className="block font-medium">Disciplina:</label>
                    {inscripcionesData && inscripcionesData.length > 0 ? (
                        <Field
                            as="select"
                            name="inscripcionId"
                            className="border p-2 w-full"
                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                                const selectedInscripcionId = e.target.value;
                                setFieldValue("inscripcionId", selectedInscripcionId);
                                if (selectedInscripcionId) {
                                    const insc = inscripcionesData.find(
                                        (ins) => ins.id.toString() === selectedInscripcionId
                                    );
                                    if (insc) {
                                        setFieldValue("disciplina", insc.disciplina.nombre);
                                    }
                                } else {
                                    setFieldValue("disciplina", "");
                                }
                            }}
                        >
                            <option value="">Seleccione una disciplina</option>
                            {inscripcionesData.map((insc: InscripcionResponse) => (
                                <option key={insc.id} value={insc.id.toString()}>
                                    {insc.disciplina.nombre}
                                </option>
                            ))}
                        </Field>
                    ) : (
                        <Field
                            as="select"
                            name="disciplina"
                            className="border p-2 w-full"
                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                                setFieldValue("disciplina", e.target.value);
                                setFieldValue("tarifa", "");
                            }}
                        >
                            <option value="">Seleccione una disciplina</option>
                            {disciplinas.map((disc: DisciplinaDetalleResponse) => (
                                <option key={disc.id} value={disc.id.toString()}>
                                    {disc.nombre}
                                </option>
                            ))}
                        </Field>
                    )}
                </div>
                <div>
                    <label className="block font-medium">Tarifa:</label>
                    <Field as="select" name="tarifa" className="border p-2 w-full">
                        <option value="">Seleccione una tarifa</option>
                        <option value="CUOTA">CUOTA</option>
                        <option value="CLASE_SUELTA">CLASE SUELTA</option>
                        <option value="CLASE_PRUEBA">CLASE DE PRUEBA</option>
                    </Field>
                </div>
                {values.tarifa === "CUOTA" && (
                    <div>
                        <label className="block font-medium">Periodo Mensual:</label>
                        <Field as="select" name="periodoMensual" className="border p-2 w-full">
                            <option value="">Seleccione el mes/periodo</option>
                            {generatePeriodos(12).map((periodo, index) => (
                                <option key={index} value={periodo}>
                                    {periodo}
                                </option>
                            ))}
                        </Field>
                    </div>
                )}
                <div className="sm:col-span-2">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block font-medium">Concepto:</label>
                            <Field as="select" name="conceptoSeleccionado" className="border p-2 w-full">
                                <option value="">Seleccione un concepto</option>
                                {conceptos.map((conc: ConceptoResponse) => (
                                    <option key={conc.id} value={conc.id}>
                                        {conc.descripcion}
                                    </option>
                                ))}
                            </Field>
                        </div>
                        <div>
                            <label className="block font-medium">Stock:</label>
                            <Field as="select" name="stockSeleccionado" className="border p-2 w-full">
                                <option value="">Seleccione un stock</option>
                                {stocks.map((s: StockResponse) => (
                                    <option key={s.id} value={s.id}>
                                        {s.nombre}
                                    </option>
                                ))}
                            </Field>
                        </div>
                    </div>
                </div>
                <div>
                    <label className="block font-medium">Cantidad:</label>
                    <Field name="cantidad" type="number" className="border p-2 w-full" min="1" />
                </div>
            </div>
            <div className="mb-4">
                <button
                    type="button"
                    className="bg-green-500 text-white p-2 rounded mt-4"
                    onClick={() => handleAgregarDetalle(values, setFieldValue)}
                >
                    Agregar Detalle
                </button>
            </div>
        </div>
    );

const DetallesTable: React.FC = () => (
    <FieldArray name="detallePagos">
        {({ remove, form }) => (
            <div className="overflow-x-auto">
                <table className="border mb-4 w-auto table-layout-auto">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">Código</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[120px]">Concepto</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">Cantidad</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">Valor Base</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">Bonificación</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">Recargo</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">Importe</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[80px]">A Cobrar</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700" style={{ display: "none" }}>Abono</th>
                            <th className="border p-2 text-center text-sm font-medium text-gray-700 min-w-[100px]">Acciones</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {form.values.detallePagos && form.values.detallePagos.length > 0 ? (
                            form.values.detallePagos.map((_: any, index: number) => (
                                <tr key={index} className="hover:bg-gray-50">
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.codigoConcepto`} type="text" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.concepto`} type="text" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.cuota`} type="text" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.valorBase`} type="number" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.montoBonificacion`} type="number" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.recargoId`} type="number" className="w-full px-2 py-1 border rounded text-center" readOnly />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.importe`}>
                                            {({ field, form }: any) => (
                                                <input
                                                    type="number"
                                                    {...field}
                                                    className="w-full px-2 py-1 border rounded text-center"
                                                    onChange={(e) => {
                                                        const newImporte = Number(e.target.value);
                                                        form.setFieldValue(`detallePagos.${index}.importe`, newImporte);
                                                        form.setFieldValue(`detallePagos.${index}.aCobrar`, newImporte);
                                                        form.setFieldValue(`detallePagos.${index}.autoGenerated`, false);
                                                    }}
                                                />
                                            )}
                                        </Field>
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <Field name={`detallePagos.${index}.aCobrar`}>
                                            {({ field, form }: any) => (
                                                <input
                                                    type="number"
                                                    {...field}
                                                    className="w-full px-2 py-1 border rounded text-center"
                                                    onChange={(e) => {
                                                        const newACobrar = Number(e.target.value);
                                                        form.setFieldValue(`detallePagos.${index}.aCobrar`, newACobrar);
                                                        form.setFieldValue(`detallePagos.${index}.autoGenerated`, false);
                                                    }}
                                                />
                                            )}
                                        </Field>
                                    </td>
                                    <td className="border p-2 text-center text-sm" style={{ display: "none" }}>
                                        <Field name={`detallePagos.${index}.abono`} type="number" className="w-full px-2 py-1 border rounded text-center" />
                                    </td>
                                    <td className="border p-2 text-center text-sm">
                                        <button
                                            type="button"
                                            className="bg-red-500 hover:bg-red-600 text-white p-1 rounded text-xs transition-colors mx-auto block"
                                            onClick={() => {
                                                if (form.values.detallePagos[index].autoGenerated && form.values.detallePagos[index].id) {
                                                    const currentRemoved = form.values.autoRemoved || [];
                                                    const newRemoved = [...currentRemoved, form.values.detallePagos[index].id];
                                                    form.setFieldValue("autoRemoved", newRemoved);
                                                }
                                                remove(index);
                                            }}
                                        >
                                            Eliminar
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={10} className="text-center text-sm p-2">
                                    No hay datos
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        )}
    </FieldArray>
);

// --- Componente principal ---
const CobranzasForm: React.FC = () => {
    const [initialValues, setInitialValues] = useState<CobranzasFormValues>(defaultValues);
    const [, setMatricula] = useState<MatriculaResponse | null>(null);
    const [selectedAlumnoId, setSelectedAlumnoId] = useState<number | null>(null);
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const { alumnos, disciplinas, stocks, metodosPago, conceptos } = useCobranzasData();
    const inscripcionesQuery = useInscripcionesActivas(selectedAlumnoId || 0);
    const { deudas, ultimoPago } = useAlumnoData(selectedAlumnoId || 0);

    const handleAlumnoChange = useCallback(
        async (
            alumnoIdStr: string,
            _currentValues: CobranzasFormValues,
            setFieldValue: (field: string, value: any, shouldValidate?: boolean) => Promise<void | FormikErrors<CobranzasFormValues>>
        ) => {
            await setFieldValue("alumno", alumnoIdStr);
            await setFieldValue("alumnoId", alumnoIdStr);
            setSelectedAlumnoId(Number(alumnoIdStr));
            await setFieldValue("matriculaRemoved", false);
            if (Number(alumnoIdStr)) {
                try {
                    const response = await matriculasApi.obtenerMatricula(Number(alumnoIdStr));
                    setMatricula(response);
                } catch (error) {
                    toast.error("Error al cargar matrícula");
                }
            }
        },
        []
    );

    const generatePeriodos = (numMeses = 12): string[] => {
        const periodos: string[] = [];
        const currentDate = new Date(new Date().toLocaleString("en-US", { timeZone: "America/Argentina/Buenos_Aires" }));
        for (let i = 0; i < numMeses; i++) {
            const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - 2 + i, 1);
            const periodo = date.toLocaleString("default", { month: "long", year: "numeric" }).toUpperCase();
            periodos.push(periodo);
        }
        return periodos;
    };

    useEffect(() => {
        const idParam = searchParams.get("id");
        if (idParam) {
            console.log("[CobranzasForm] Cargando datos del pago con id:", idParam);
            pagosApi
                .obtenerPagoPorId(Number(idParam))
                .then((pagoData) => {
                    console.log("[CobranzasForm] Datos recibidos:", pagoData);
                    setInitialValues({
                        id: pagoData.id,
                        reciboNro: pagoData.id.toString(),
                        alumno: pagoData.alumnoId ? pagoData.alumnoId.toString() : "",
                        inscripcionId: pagoData.inscripcionId ? pagoData.inscripcionId.toString() : "",
                        alumnoId: pagoData.alumnoId ? pagoData.alumnoId.toString() : "",
                        fecha: pagoData.fecha || new Date().toISOString().split("T")[0],
                        detallePagos: pagoData.detallePagos.map((detalle: any) => ({
                            id: detalle.id,
                            codigoConcepto: detalle.codigoConcepto,
                            concepto: detalle.concepto,
                            cuota: detalle.cuota,
                            valorBase: detalle.valorBase,
                            bonificacionId: detalle.bonificacion ? detalle.bonificacion.id.toString() : "",
                            importe: detalle.importe,
                            aCobrar: detalle.aCobrar != null ? detalle.aCobrar : detalle.importe,
                            aFavor: detalle.aFavor != null ? detalle.aFavor : 0,
                            autoGenerated: true,
                        })),
                        totalCobrado: pagoData.detallePagos.reduce(
                            (sum: number, detalle: any) => sum + (Number(detalle.aCobrar) || Number(detalle.importe) || 0),
                            0
                        ),
                        totalACobrar: pagoData.detallePagos.reduce(
                            (sum: number, detalle: any) => sum + (Number(detalle.importe) || 0),
                            0
                        ),
                        metodoPagoId: pagoData.metodoPago ? pagoData.metodoPago.toString() : "",
                        observaciones: pagoData.observaciones || "",
                        conceptoSeleccionado: "",
                        stockSeleccionado: "",
                        matriculaRemoved: false,
                        mensualidadId: "",
                        disciplina: "",
                        tarifa: "",
                        cantidad: 1,
                        periodoMensual: getMesVigente(),
                        autoRemoved: [],
                    });
                    console.log("[CobranzasForm] Valores iniciales actualizados:", {
                        ...pagoData,
                        autoRemoved: [],
                    });
                })
                .catch(() => {
                    toast.error("Error al cargar los datos del pago.");
                });
        }
    }, [searchParams]);

    const handleAgregarDetalle = (values: CobranzasFormValues, setFieldValue: any) => {
        const newDetails = [...values.detallePagos];
        let added = false;
        const isDuplicate = (concept: string) =>
            newDetails.some((detalle) => detalle.concepto === concept);

        if (values.conceptoSeleccionado) {
            const selectedConcept = conceptos.find(
                (c: ConceptoResponse) => c.id.toString() === values.conceptoSeleccionado
            );
            if (selectedConcept) {
                if (isDuplicate(selectedConcept.descripcion)) {
                    toast.error("Concepto ya se encuentra agregado");
                } else {
                    newDetails.push({
                        id: null,
                        codigoConcepto: Number(selectedConcept.id),
                        concepto: selectedConcept.descripcion,
                        cuota: "1",
                        valorBase: selectedConcept.precio,
                        bonificacionId: "",
                        recargoId: "",
                        importe: selectedConcept.precio,
                        aCobrar: selectedConcept.precio,
                        autoGenerated: false,
                        aFavor: 0,
                    });
                    added = true;
                }
            }
        }
        if ((values.inscripcionId || values.disciplina) && values.tarifa) {
            let inscSeleccionada: InscripcionResponse | undefined;
            if (values.inscripcionId) {
                inscSeleccionada = inscripcionesQuery.data?.find(
                    (insc) => insc.id.toString() === values.inscripcionId
                );
            }
            if (inscSeleccionada) {
                let precio = 0;
                let tarifaLabel = "";
                if (values.tarifa === "CUOTA") {
                    precio = inscSeleccionada.disciplina.valorCuota;
                    tarifaLabel = "CUOTA";
                } else if (values.tarifa === "CLASE_SUELTA") {
                    precio = inscSeleccionada.disciplina.claseSuelta || 0;
                    tarifaLabel = "CLASE SUELTA";
                } else if (values.tarifa === "CLASE_PRUEBA") {
                    precio = inscSeleccionada.disciplina.clasePrueba || 0;
                    tarifaLabel = "CLASE DE PRUEBA";
                }
                const cantidad = Number(values.cantidad) || 1;
                const total = precio * cantidad;
                const conceptoDetalle = `${inscSeleccionada.disciplina.nombre} - ${tarifaLabel} - ${values.periodoMensual}`;
                if (isDuplicate(conceptoDetalle)) {
                    toast.error("Concepto ya se encuentra agregado");
                } else {
                    newDetails.push({
                        id: null,
                        codigoConcepto: inscSeleccionada.disciplina.id,
                        concepto: conceptoDetalle,
                        cuota: cantidad.toString(),
                        valorBase: total,
                        bonificacionId: "",
                        recargoId: "",
                        importe: total,
                        aCobrar: total,
                        autoGenerated: false,
                        aFavor: 0,
                    });
                    added = true;
                }
            }
        }
        if (values.stockSeleccionado) {
            const selectedStock = stocks.find(
                (s: StockResponse) => s.id.toString() === values.stockSeleccionado
            );
            if (selectedStock) {
                const cantidad = Number(values.cantidad) || 1;
                const total = selectedStock.precio * cantidad;
                const conceptoStock = selectedStock.nombre;
                if (isDuplicate(conceptoStock)) {
                    toast.error("Concepto ya se encuentra agregado");
                } else {
                    newDetails.push({
                        id: null,
                        codigoConcepto: selectedStock.id,
                        concepto: conceptoStock,
                        cuota: cantidad.toString(),
                        valorBase: total,
                        bonificacionId: "",
                        recargoId: "",
                        importe: total,
                        aCobrar: total,
                        autoGenerated: false,
                        aFavor: 0,
                    });
                    added = true;
                }
            }
        }
        if (added) {
            setFieldValue("detallePagos", newDetails);
            const nuevoTotalCobrado = newDetails.reduce(
                (acc, det) => acc + (Number(det.aCobrar) || 0),
                0
            );
            setFieldValue("totalCobrado", nuevoTotalCobrado);
            setFieldValue("inscripcionId", "");
            setFieldValue("disciplina", "");
            setFieldValue("tarifa", "");
            setFieldValue("conceptoSeleccionado", "");
            setFieldValue("stockSeleccionado", "");
            setFieldValue("cantidad", 1);
        } else if (
            !values.conceptoSeleccionado &&
            !(values.inscripcionId || values.disciplina) &&
            !values.stockSeleccionado
        ) {
            toast.error("Seleccione al menos un conjunto de campos para agregar");
        }
    };

    const onSubmit = async (values: CobranzasFormValues, actions: any) => {
        try {
            const detallesFiltrados = values.detallePagos.filter((detalle) =>
                detalle.id !== null ? true : Number(detalle.importe) !== 0
            );
            const pagoRegistroRequest: PagoRegistroRequest = {
                fecha: values.fecha,
                fechaVencimiento: values.fecha,
                monto: Number(values.totalACobrar),
                inscripcionId: values.inscripcionId ? Number(values.inscripcionId) : -1,
                alumnoId: values.alumnoId ? Number(values.alumnoId) : undefined,
                metodoPagoId: values.metodoPagoId ? Number(values.metodoPagoId) : undefined,
                recargoAplicado: values.metodoPagoId
                    ? metodosPago.find(
                        (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
                    )?.descripcion.toUpperCase() === "DEBITO"
                    : false,
                bonificacionAplicada: false,
                detallePagos: detallesFiltrados.map((d) => ({
                    id: d.id, // si existe, se envía
                    codigoConcepto: d.codigoConcepto ? String(d.codigoConcepto) : undefined,
                    concepto: d.concepto,
                    cuota: d.cuota,
                    valorBase: d.valorBase,
                    bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : undefined,
                    recargoId: d.recargoId ? Number(d.recargoId) : undefined,
                    aCobrar: d.aCobrar, // enviamos lo que haya en el form, sin modificar
                    importe: d.importe, // valor enviado por el usuario
                })),
                pagoMedios: [],
                pagoMatricula: false,
                activo: true,
            };

            console.log("[onSubmit] Valores del formulario:", values);
            console.log("[onSubmit] Payload a enviar:", pagoRegistroRequest);

            if (values.id && values.id !== 0) {
                console.log("[onSubmit] Se detecta id existente. Actualizando pago con id:", values.id);
                await pagosApi.actualizarPago(Number(values.id), pagoRegistroRequest);
                toast.success("Cobranza actualizada correctamente");
            } else {
                console.log("[onSubmit] No existe id. Registrando nuevo pago.");
                await pagosApi.registrarPago(pagoRegistroRequest);
                toast.success("Cobranza registrada correctamente");
            }
            actions.resetForm();
            navigate("/pagos");
        } catch (error) {
            console.error("[onSubmit] Error en registro/actualización:", error);
            toast.error("Error al registrar/actualizar la cobranza");
        }
    };

    return (
        <ResponsiveContainer className="py-4">
            <h1 className="page-title text-2xl font-bold mb-4">Gestión de Cobranzas</h1>
            {ultimoPago && (
                <div className="mb-4 p-2 border">
                    <p>
                        Último pago registrado: <strong>{ultimoPago.id}</strong>
                    </p>
                    <p>Monto: {ultimoPago.monto}</p>
                </div>
            )}
            <Formik
                initialValues={initialValues}
                validationSchema={validationSchema}
                onSubmit={onSubmit}
                enableReinitialize
            >
                {({ values, setFieldValue }) => (
                    <Form className="w-full">
                        <FormValuesUpdater newValues={initialValues} />
                        <TotalsUpdater metodosPago={metodosPago} />
                        {/* Integración de PaymentIdUpdater para actualizar el field "id" */}
                        <PaymentIdUpdater ultimoPago={ultimoPago} />
                        {deudas && <AutoAddDetalles deudaData={deudas} />}
                        <FormHeader alumnos={alumnos} handleAlumnoChange={handleAlumnoChange} />
                        <ConceptosSection
                            inscripcionesData={inscripcionesQuery.data}
                            disciplinas={disciplinas}
                            stocks={stocks}
                            conceptos={conceptos}
                            generatePeriodos={generatePeriodos}
                            values={values}
                            setFieldValue={setFieldValue}
                            handleAgregarDetalle={handleAgregarDetalle}
                        />
                        <DetallesTable />
                        <div className="border p-4 mb-4">
                            <h2 className="font-bold mb-2">Totales y Pago</h2>
                            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                                <div>
                                    <label className="block font-medium">Total a Cobrar:</label>
                                    <input
                                        type="number"
                                        readOnly
                                        className="border p-2 w-full"
                                        value={values.totalACobrar}
                                    />
                                    {values.metodoPagoId &&
                                        (() => {
                                            const selectedMetodo = metodosPago.find(
                                                (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
                                            );
                                            const isDebit =
                                                selectedMetodo && selectedMetodo.descripcion.toUpperCase() === "DEBITO";
                                            return (
                                                isDebit && (
                                                    <p className="text-sm text-info">
                                                        Se ha agregado recargo de ${selectedMetodo.recargo} por DEBITO.
                                                    </p>
                                                )
                                            );
                                        })()}
                                </div>
                                <div>
                                    <label className="block font-medium">Total Cobrado:</label>
                                    <Field
                                        name="totalCobrado"
                                        type="number"
                                        className="border p-2 w-full"
                                        onBlur={(e: React.FocusEvent<HTMLInputElement>) => {
                                            const totalInput = Number(e.target.value);
                                            setFieldValue("totalCobrado", totalInput);
                                        }}
                                    />
                                    <small className="text-gray-500">
                                        Se autocompleta sumando el valor de A Cobrar de cada detalle, pero puedes modificarlo.
                                    </small>
                                </div>
                                <div>
                                    <label className="block font-medium">Método de Pago:</label>
                                    <Field as="select" name="metodoPagoId" className="border p-2 w-full">
                                        <option value="">Seleccione un método de pago</option>
                                        {metodosPago.map((mp: MetodoPagoResponse) => (
                                            <option key={mp.id} value={mp.id}>
                                                {mp.descripcion}
                                            </option>
                                        ))}
                                    </Field>
                                </div>
                            </div>
                        </div>
                        <div className="border p-4 mb-4">
                            <label className="block font-medium">Observaciones:</label>
                            <Field as="textarea" name="observaciones" className="border p-2 w-full" rows="3" />
                        </div>
                        <div className="flex justify-end gap-4">
                            <button type="submit" className="bg-green-500 p-2 rounded">
                                {searchParams.get("id") ? "Actualizar Cobranza" : "Registrar Cobranza"}
                            </button>
                            <button type="reset" className="bg-gray-500 p-2 rounded">
                                Cancelar
                            </button>
                        </div>
                    </Form>
                )}
            </Formik>
        </ResponsiveContainer>
    );
};

export default CobranzasForm;
