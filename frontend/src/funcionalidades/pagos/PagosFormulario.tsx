// CobranzasForm.tsx
import React, { useState, useCallback, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import {
    Formik,
    Form,
    Field,
    FieldArray,
    FormikHelpers,
    useFormikContext,
    FieldProps,
    FormikErrors,
} from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import matriculasApi from "../../api/matriculasApi";
import { useCobranzasData } from "../../hooks/useCobranzasData";
import { useAlumnoDeudas } from "../../hooks/useAlumnoDeudas";
import { useUltimoPago } from "../../hooks/useUltimoPago";
import { useCargaMensualidades } from "../../hooks/useCargaMensualidades";
import type {
    AlumnoListadoResponse,
    StockResponse,
    PagoRegistroRequest,
    ConceptoResponse,
    MatriculaResponse,
    CobranzasFormValues,
    MetodoPagoResponse,
    ReporteMensualidadDTO,
} from "../../types/types";
import { AutoAddDetalles } from "../../hooks/context/AutoAddDetalles";

// Función para obtener el periodo vigente (mes y año) en formato "NombreMes Año"
const getMesVigente = () =>
    new Date().toLocaleString("default", { month: "long", year: "numeric" });

// Valores iniciales del formulario
const defaultValues: CobranzasFormValues = {
    id: 0,
    reciboNro: "AUTO-001",
    alumno: "",
    inscripcionId: "",
    fecha: new Date().toISOString().split("T")[0],
    detallePagos: [],
    disciplina: "",
    tarifa: "",
    conceptoSeleccionado: "",
    stockSeleccionado: "",
    cantidad: 1,
    totalCobrado: 0,
    metodoPagoId: "",
    observaciones: "",
    matriculaRemoved: false,
    mensualidadId: "",
    periodoMensual: getMesVigente(),
};

// Esquema de validación
const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria"),
});

// Componente para actualizar los valores de Formik sin reinicializar el formulario
const FormValuesUpdater: React.FC<{ newValues: Partial<CobranzasFormValues> }> = ({
    newValues,
}) => {
    const { values, setValues } = useFormikContext<CobranzasFormValues>();

    useEffect(() => {
        // Actualiza solo las propiedades que han cambiado
        setValues({ ...values, ...newValues });
    }, [newValues, setValues]);

    return null;
};

const CobranzasForm: React.FC = () => {
    const [initialValues, setInitialValues] =
        useState<CobranzasFormValues>(defaultValues);
    const [matricula, setMatricula] = useState<MatriculaResponse | null>(null);
    const [mensualidades, setMensualidades] = useState<ReporteMensualidadDTO[]>([]);
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { alumnos, stocks, metodosPago, conceptos } = useCobranzasData();
    const { loadDeudasForAlumno } = useAlumnoDeudas();
    const [selectedAlumnoId, setSelectedAlumnoId] = useState<number | null>(null);
    const ultimoPago = useUltimoPago(selectedAlumnoId);
    const [inscripciones] = useState<any[]>([]);
    const { loadMensualidadesAlumno } = useCargaMensualidades(alumnos);

    const loadMatricula = async (alumnoId: number) => {
        console.log("Cargando matrícula para alumno", alumnoId);
        const response = await matriculasApi.obtenerMatricula(alumnoId);
        console.log("Matrícula recibida:", response);
        setMatricula(response);
        return response;
    };

    // Actualizamos la firma para que coincida con la de Formik
    const loadInscripciones = async (
        alumnoId: number,
        setFieldValue: (
            field: string,
            value: any,
            shouldValidate?: boolean
        ) => Promise<void | FormikErrors<CobranzasFormValues>>
    ) => {
        console.log("Cargando inscripciones para alumno", alumnoId);
        const inscripcionesApi = await import("../../api/inscripcionesApi");
        const inscripcionesActivas =
            await inscripcionesApi.default.obtenerInscripcionesActivas(alumnoId);
        console.log("Inscripciones activas:", inscripcionesActivas);
        await setFieldValue(
            "inscripcionId",
            inscripcionesActivas.length > 0 ? inscripcionesActivas[0].id : ""
        );
        return inscripcionesActivas;
    };

    // Actualizamos la firma de handleAlumnoChange igualmente
    const handleAlumnoChange = useCallback(
        async (
            alumnoId: string,
            _currentValues: CobranzasFormValues,
            setFieldValue: (
                field: string,
                value: any,
                shouldValidate?: boolean
            ) => Promise<void | FormikErrors<CobranzasFormValues>>
        ) => {
            console.log("Alumno cambiado:", alumnoId);
            const numAlumnoId = Number(alumnoId);
            await setFieldValue("alumno", alumnoId);
            setSelectedAlumnoId(numAlumnoId);
            await setFieldValue("matriculaRemoved", false);

            try {
                await loadInscripciones(numAlumnoId, setFieldValue);
                await loadMatricula(numAlumnoId);
                const mensualidadesRaw = await loadMensualidadesAlumno(numAlumnoId);
                console.log("Mensualidades recibidas:", mensualidadesRaw);
                setMensualidades(mensualidadesRaw);
                loadDeudasForAlumno(numAlumnoId, setFieldValue);
            } catch (err) {
                console.error("Error al cargar datos del alumno:", err);
            }
        },
        [loadDeudasForAlumno, loadMensualidadesAlumno]
    );

    // Actualiza los valores del formulario cuando se obtiene el último pago (solo en creación)
    useEffect(() => {
        if (!searchParams.get("id") && selectedAlumnoId && ultimoPago) {
            console.log("Estableciendo valores iniciales a partir del último pago:", ultimoPago);
            setInitialValues((prev) => ({
                ...prev,
                id: ultimoPago.id,
                reciboNro: ultimoPago.id.toString(),
                alumno: ultimoPago.alumnoId.toString(),
                inscripcionId: ultimoPago.inscripcionId.toString(),
                fecha: ultimoPago.fecha || new Date().toISOString().split("T")[0],
                detallePagos: ultimoPago.detallePagos.map((detalle: any) => ({
                    id: detalle.id,
                    codigoConcepto: detalle.codigoConcepto,
                    concepto: detalle.concepto,
                    cuota: detalle.cuota,
                    valorBase: detalle.valorBase,
                    bonificacionId: detalle.bonificacion ? detalle.bonificacion.toString() : "",
                    recargoId: detalle.recargo ? detalle.recargo.toString() : "",
                    aFavor: detalle.aFavor || 0,
                    importe: detalle.importe,
                    aCobrar: detalle.aCobrar != null ? detalle.aCobrar : detalle.importe,
                    abono: 0,
                    autoGenerated: false,
                })),
                totalCobrado: ultimoPago.detallePagos.reduce(
                    (sum: number, detalle: any) =>
                        sum + (Number(detalle.aCobrar) || Number(detalle.importe) || 0),
                    0
                ),
                metodoPagoId: ultimoPago.metodoPago ? ultimoPago.metodoPago.toString() : "",
                observaciones: ultimoPago.observaciones || "",
                conceptoSeleccionado: "",
                stockSeleccionado: "",
                matriculaRemoved: false,
                mensualidadId: "",
                disciplina: "",
                tarifa: "",
                cantidad: 1,
                periodoMensual: getMesVigente(),
            }));
        }
    }, [selectedAlumnoId, ultimoPago, searchParams]);

    // Actualiza los valores para edición si existe el parámetro "id"
    useEffect(() => {
        const idParam = searchParams.get("id");
        if (idParam) {
            console.log("Cargando pago para edición con id", idParam);
            pagosApi
                .obtenerPagoPorId(Number(idParam))
                .then((pagoData) => {
                    console.log("Pago recibido para edición:", pagoData);
                    setInitialValues((prev) => ({
                        ...prev,
                        id: pagoData.id,
                        reciboNro: pagoData.id.toString(),
                        alumno: pagoData.alumnoId ? pagoData.alumnoId.toString() : "",
                        inscripcionId: pagoData.inscripcionId.toString(),
                        fecha: pagoData.fecha || new Date().toISOString().split("T")[0],
                        detallePagos: pagoData.detallePagos.map((detalle: any) => ({
                            id: detalle.id,
                            codigoConcepto: detalle.codigoConcepto,
                            concepto: detalle.concepto,
                            cuota: detalle.cuota,
                            valorBase: detalle.valorBase,
                            bonificacionId: detalle.bonificacion ? detalle.bonificacion.toString() : "",
                            recargoId: detalle.recargo ? detalle.recargo.toString() : "",
                            aFavor: detalle.aFavor || 0,
                            importe: detalle.importe,
                            aCobrar: detalle.aCobrar,
                            abono: 0,
                            autoGenerated: false,
                        })),
                        totalCobrado: pagoData.detallePagos.reduce(
                            (sum: number, detalle: any) =>
                                sum + (Number(detalle.aCobrar) || Number(detalle.importe) || 0),
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
                    }));
                })
                .catch((err) => {
                    console.error("Error al cargar el pago para edición:", err);
                    toast.error("Error al cargar los datos del pago.");
                });
        }
    }, [searchParams]);

    // Función para calcular el total a pagar (utilizando "aCobrar")
    const calculateTotalAPagar = (
        detallePagos: CobranzasFormValues["detallePagos"]
    ) =>
        detallePagos.reduce(
            (total, item) => total + Number(item.aCobrar || 0),
            0
        );

    const actualizarDetalleImporte = (detalle: any) => ({ ...detalle });

    const onSubmit = async (
        values: CobranzasFormValues,
        actions: FormikHelpers<CobranzasFormValues>
    ) => {
        try {
            const detallePagosActualizados = values.detallePagos.map(actualizarDetalleImporte);
            const detallesFiltrados = detallePagosActualizados.filter(
                (detalle) => Number(detalle.importe) !== 0
            );
            // Extraer el método de pago seleccionado para obtener el recargo
            const selectedMetodo = metodosPago.find(
                (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
            );
            const isDebit =
                selectedMetodo && selectedMetodo.descripcion.toUpperCase() === "DEBITO";
            const baseTotal = detallesFiltrados.reduce(
                (total, item) => total + Number(item.importe || 0),
                0
            );
            const recargoValue = isDebit ? Number(selectedMetodo?.recargo) : 0;
            const montoTotal = baseTotal + recargoValue;
            const totalCobrado = detallesFiltrados.reduce(
                (sum, detalle) => sum + (Number(detalle.aCobrar) || 0),
                0
            );
            const saldoRestante = montoTotal - totalCobrado;

            const pagoRegistroRequest: PagoRegistroRequest = {
                fecha: values.fecha,
                fechaVencimiento: values.fecha,
                monto: montoTotal,
                inscripcionId: Number(values.inscripcionId),
                metodoPagoId: values.metodoPagoId ? Number(values.metodoPagoId) : undefined,
                recargoAplicado: isDebit,
                bonificacionAplicada: false,
                saldoRestante: saldoRestante,
                saldoAFavor: 0,
                detallePagos: detallesFiltrados.map((d) => ({
                    id: d.id,
                    codigoConcepto: d.codigoConcepto,
                    concepto: d.concepto,
                    cuota: d.cuota,
                    valorBase: d.valorBase,
                    bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : undefined,
                    recargoId: d.recargoId ? Number(d.recargoId) : undefined,
                    aFavor: d.aFavor,
                    importe: d.importe,
                    aCobrar: d.aCobrar,
                    abono: d.abono,
                })),
                pagoMedios: [],
                pagoMatricula: false,
                activo: true,
            };

            if (values.id) {
                await pagosApi.actualizarPago(Number(values.id), pagoRegistroRequest);
                toast.success("Cobranza actualizada correctamente");
            } else {
                await pagosApi.registrarPago(pagoRegistroRequest);
                toast.success("Cobranza registrada correctamente");
            }
            actions.resetForm();
            navigate("/pagos");
        } catch (error) {
            console.error("Error al registrar/actualizar la cobranza:", error);
            toast.error("Error al registrar/actualizar la cobranza");
        }
    };

    return (
        <div className="page-container p-4">
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
            >
                {({ values, setFieldValue }) => {
                    // Dentro del render de Formik se calcula el total a pagar basado en "aCobrar"
                    const baseTotal = calculateTotalAPagar(values.detallePagos);
                    // Se busca el método de pago seleccionado (según values.metodoPagoId)
                    const selectedMetodo = metodosPago.find(
                        (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
                    );
                    const isDebit =
                        selectedMetodo && selectedMetodo.descripcion.toUpperCase() === "DEBITO";
                    const recargoValue = isDebit ? Number(selectedMetodo?.recargo) : 0;
                    const montoTotal = baseTotal + recargoValue;

                    return (
                        <Form className="max-w-5xl mx-auto">
                            {/* Actualización de valores sin reinicializar el formulario */}
                            <FormValuesUpdater newValues={initialValues} />
                            <AutoAddDetalles
                                matricula={matricula}
                                mensualidades={mensualidades}
                                conceptos={conceptos}
                            />
                            {/* Datos de Cobranza */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Cobranza</h2>
                                <div className="grid grid-cols-3 gap-4">
                                    <div>
                                        <label className="block font-medium">Recibo Nro:</label>
                                        <Field name="reciboNro" readOnly className="border p-2 w-full" />
                                    </div>
                                    <div>
                                        <label className="block font-medium">Alumno:</label>
                                        <Field
                                            as="select"
                                            name="alumno"
                                            className="border p-2 w-full"
                                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                                                handleAlumnoChange(e.target.value, values, setFieldValue)
                                            }
                                        >
                                            <option value="">Seleccione un alumno</option>
                                            {alumnos.map((alumno: AlumnoListadoResponse) => (
                                                <option key={alumno.id} value={alumno.id}>
                                                    {alumno.nombre} {alumno.apellido}
                                                </option>
                                            ))}
                                        </Field>
                                    </div>
                                    <div>
                                        <label className="block font-medium">Fecha:</label>
                                        <Field name="fecha" type="date" className="border p-2 w-full" />
                                    </div>
                                </div>
                            </div>
                            {/* Sección de Disciplina y Conceptos */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Disciplina y Conceptos</h2>
                                <div className="grid grid-cols-4 gap-4 items-end">
                                    <div className="col-span-2">
                                        <label className="block font-medium">Disciplina:</label>
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
                                            {inscripciones.map((insc) => (
                                                <option key={insc.id} value={insc.disciplina.id}>
                                                    {insc.disciplina.nombre}
                                                </option>
                                            ))}
                                        </Field>
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
                                            <Field
                                                as="select"
                                                name="periodoMensual"
                                                className="border p-2 w-full"
                                            >
                                                <option value="">Seleccione el mes/periodo</option>
                                                <option value="Enero 2025">Enero 2025</option>
                                                <option value="Febrero 2025">Febrero 2025</option>
                                                <option value="Marzo 2025">Marzo 2025</option>
                                                <option value="Abril 2025">Abril 2025</option>
                                                <option value="Mayo 2025">Mayo 2025</option>
                                            </Field>
                                        </div>
                                    )}
                                    <div className="col-span-2">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="block font-medium">Concepto:</label>
                                                <Field
                                                    as="select"
                                                    name="conceptoSeleccionado"
                                                    className="border p-2 w-full"
                                                >
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
                                                <Field
                                                    as="select"
                                                    name="stockSeleccionado"
                                                    className="border p-2 w-full"
                                                >
                                                    <option value="">Seleccione un stock</option>
                                                    {stocks.map((prod: StockResponse) => (
                                                        <option key={prod.id} value={prod.id}>
                                                            {prod.nombre}
                                                        </option>
                                                    ))}
                                                </Field>
                                            </div>
                                        </div>
                                    </div>
                                    <div>
                                        <label className="block font-medium">Cantidad:</label>
                                        <Field
                                            name="cantidad"
                                            type="number"
                                            className="border p-2 w-full"
                                            min="1"
                                        />
                                    </div>
                                </div>
                            </div>
                            {/* Botón para agregar detalle manual */}
                            <div className="col-span-4 mb-4">
                                <button
                                    type="button"
                                    className="bg-green-500 text-white p-2 rounded mt-4"
                                    onClick={() => {
                                        console.log("Agregando detalle manual...");
                                        const newDetails = [...values.detallePagos];
                                        // Agregar detalle manual por concepto seleccionado
                                        if (values.conceptoSeleccionado) {
                                            const selectedConcept = conceptos.find(
                                                (c: ConceptoResponse) =>
                                                    c.id.toString() === values.conceptoSeleccionado
                                            );
                                            if (selectedConcept) {
                                                newDetails.push({
                                                    id: null,
                                                    codigoConcepto: Number(selectedConcept.id),
                                                    concepto: selectedConcept.descripcion,
                                                    cuota: "1",
                                                    valorBase: selectedConcept.precio,
                                                    bonificacionId: "",
                                                    recargoId: "",
                                                    aFavor: 0,
                                                    importe: selectedConcept.precio,
                                                    aCobrar: selectedConcept.precio,
                                                    abono: 0,
                                                    autoGenerated: false,
                                                });
                                                console.log("Detalle manual agregado:", newDetails[newDetails.length - 1]);
                                            }
                                        }
                                        // Agregar detalle manual por disciplina y tarifa
                                        if (values.disciplina && values.tarifa) {
                                            const inscripcionSeleccionada = inscripciones.find(
                                                (insc) =>
                                                    insc.disciplina.id.toString() === values.disciplina
                                            );
                                            if (inscripcionSeleccionada) {
                                                let precio = 0;
                                                let tarifaLabel = "";
                                                if (values.tarifa === "CUOTA") {
                                                    precio = inscripcionSeleccionada.disciplina.valorCuota;
                                                    tarifaLabel = "CUOTA";
                                                } else if (values.tarifa === "CLASE_SUELTA") {
                                                    precio = inscripcionSeleccionada.disciplina.claseSuelta || 0;
                                                    tarifaLabel = "CLASE SUELTA";
                                                } else if (values.tarifa === "CLASE_PRUEBA") {
                                                    precio = inscripcionSeleccionada.disciplina.clasePrueba || 0;
                                                    tarifaLabel = "CLASE DE PRUEBA";
                                                }
                                                const cantidad = Number(values.cantidad) || 1;
                                                const total = precio * cantidad;
                                                newDetails.push({
                                                    id: null,
                                                    codigoConcepto: inscripcionSeleccionada.disciplina.id,
                                                    concepto: `${inscripcionSeleccionada.disciplina.nombre.toUpperCase()} - ${tarifaLabel} - ${values.periodoMensual}`,
                                                    cuota: cantidad.toString(),
                                                    valorBase: total,
                                                    bonificacionId: "",
                                                    recargoId: "",
                                                    aFavor: 0,
                                                    importe: total,
                                                    aCobrar: total,
                                                    abono: 0,
                                                    autoGenerated: false,
                                                });
                                                console.log("Detalle por disciplina agregado:", newDetails[newDetails.length - 1]);
                                            }
                                        }
                                        // Agregar detalle manual por stock seleccionado
                                        if (values.stockSeleccionado) {
                                            const selectedStock = stocks.find(
                                                (s: StockResponse) =>
                                                    s.id.toString() === values.stockSeleccionado
                                            );
                                            if (selectedStock) {
                                                const cantidad = Number(values.cantidad) || 1;
                                                const total = selectedStock.precio * cantidad;
                                                newDetails.push({
                                                    id: null,
                                                    codigoConcepto: selectedStock.id,
                                                    concepto: selectedStock.nombre,
                                                    cuota: cantidad.toString(),
                                                    valorBase: total,
                                                    bonificacionId: "",
                                                    recargoId: "",
                                                    aFavor: 0,
                                                    importe: total,
                                                    aCobrar: total,
                                                    abono: 0,
                                                    autoGenerated: false,
                                                });
                                                console.log("Detalle por stock agregado:", newDetails[newDetails.length - 1]);
                                            }
                                        }
                                        if (newDetails.length > values.detallePagos.length) {
                                            setFieldValue("id", null);
                                            setFieldValue("detallePagos", newDetails);
                                            const nuevoTotalCobrado = newDetails.reduce(
                                                (acc, det) => acc + (Number(det.aCobrar) || 0),
                                                0
                                            );
                                            setFieldValue("totalCobrado", nuevoTotalCobrado);
                                            setFieldValue("disciplina", "");
                                            setFieldValue("tarifa", "");
                                            setFieldValue("conceptoSeleccionado", "");
                                            setFieldValue("stockSeleccionado", "");
                                            setFieldValue("cantidad", 1);
                                        } else {
                                            toast.error("Seleccione al menos un conjunto de campos para agregar");
                                        }
                                    }}
                                >
                                    Agregar Detalle
                                </button>
                            </div>
                            {/* Detalle de Facturación */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Detalles de Facturación</h2>
                                <FieldArray name="detallePagos">
                                    {({ remove }) => (
                                        <table className="min-w-full border mb-4">
                                            <thead>
                                                <tr>
                                                    <th className="border p-2">Código (ID)</th>
                                                    <th className="border p-2">Concepto</th>
                                                    <th className="border p-2">Cuota/Cantidad</th>
                                                    <th className="border p-2">Valor Base</th>
                                                    <th className="border p-2">Bonificación</th>
                                                    <th className="border p-2">Recargo</th>
                                                    <th className="border p-2">Importe</th>
                                                    <th className="border p-2">A Cobrar</th>
                                                    <th className="border p-2" style={{ display: "none" }}>Abono</th>
                                                    <th className="border p-2">Acciones</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {values.detallePagos && values.detallePagos.length > 0 ? (
                                                    values.detallePagos.map((_detalle, index) => (
                                                        <tr key={index}>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.codigoConcepto`}
                                                                    type="text"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.concepto`}
                                                                    type="text"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.cuota`}
                                                                    type="text"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.valorBase`}
                                                                    type="number"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.bonificacionId`}
                                                                    type="number"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.recargoId`}
                                                                    type="number"
                                                                    className="w-full"
                                                                    readOnly
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field
                                                                    name={`detallePagos.${index}.importe`}
                                                                    type="number"
                                                                    className="w-full"
                                                                />
                                                            </td>
                                                            <td className="border p-2">
                                                                <Field name={`detallePagos.${index}.aCobrar`}>
                                                                    {({ field, form }: FieldProps) => (
                                                                        <input
                                                                            type="number"
                                                                            {...field}
                                                                            className="w-full"
                                                                            onChange={(e) => {
                                                                                const inputValue = e.target.value;
                                                                                const importe = Number(values.detallePagos[index].importe) || 0;
                                                                                const newACobrar =
                                                                                    inputValue === "" ? importe : Number(inputValue);
                                                                                form.setFieldValue(`detallePagos.${index}.aCobrar`, newACobrar);
                                                                                form.setFieldValue(`detallePagos.${index}.abono`, importe - newACobrar);
                                                                                const updatedTotal = values.detallePagos.reduce(
                                                                                    (sum, d, idx) =>
                                                                                        sum + (idx === index ? newACobrar : Number(d.aCobrar) || 0),
                                                                                    0
                                                                                );
                                                                                form.setFieldValue("totalCobrado", updatedTotal);
                                                                            }}
                                                                        />
                                                                    )}
                                                                </Field>
                                                            </td>
                                                            <td className="border p-2" style={{ display: "none" }}>
                                                                <Field
                                                                    name={`detallePagos.${index}.abono`}
                                                                    type="number"
                                                                    className="w-full"
                                                                />
                                                            </td>
                                                            <td className="border p-2 text-center">
                                                                <button
                                                                    type="button"
                                                                    className="bg-red-500 text-white p-1 rounded"
                                                                    onClick={() => {
                                                                        console.log("Eliminando detalle en el índice:", index);
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
                                                        <td colSpan={9} className="text-center p-2">
                                                            No hay conceptos agregados
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </table>
                                    )}
                                </FieldArray>
                            </div>
                            {/* Totales y Pago */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Totales y Pago</h2>
                                <div className="grid grid-cols-3 gap-4">
                                    <div>
                                        <label className="block font-medium">Total a Pagar:</label>
                                        <input
                                            type="number"
                                            readOnly
                                            className="border p-2 w-full"
                                            value={montoTotal}
                                        />
                                        {isDebit && (
                                            <p className="text-sm text-info">
                                                Se ha agregado recargo de ${recargoValue} por DEBITO.
                                            </p>
                                        )}
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
                                            Se autocompleta sumando el total a pagar de cada concepto, pero puedes modificarlo.
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
                            {/* Observaciones */}
                            <div className="border p-4 mb-4">
                                <label className="block font-medium">Observaciones:</label>
                                <Field as="textarea" name="observaciones" className="border p-2 w-full" rows="3" />
                            </div>
                            {/* Botones de Acción */}
                            <div className="flex justify-end gap-4">
                                <button type="submit" className="bg-green-500 p-2 rounded">
                                    {searchParams.get("id") ? "Actualizar Cobranza" : "Registrar Cobranza"}
                                </button>
                                <button type="reset" className="bg-gray-500 p-2 rounded">
                                    Cancelar
                                </button>
                            </div>
                        </Form>
                    );
                }}
            </Formik>
        </div>
    );
};

export default CobranzasForm;
