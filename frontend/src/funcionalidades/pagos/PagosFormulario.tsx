import React, { useState, useCallback, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { Formik, Form, Field, FieldArray, useFormikContext } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import matriculasApi from "../../api/matriculasApi";
import disciplinasApi from "../../api/disciplinasApi";
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
    DisciplinaDetalleResponse,
} from "../../types/types";
import { AutoAddDetalles } from "../../hooks/context/AutoAddDetalles";
import ResponsiveContainer from "../../componentes/comunes/ResponsiveContainer";

// Función para obtener el periodo vigente (ej.: "ENERO 2025")
const getMesVigente = () =>
    new Date()
        .toLocaleString("default", {
            month: "long",
            year: "numeric",
            timeZone: "America/Argentina/Buenos_Aires",
        })
        .toUpperCase();

// Valores iniciales para el formulario de cobranza (CobranzasFormValues)
// Se incluyen "importe" y "aCobrar" para fines visuales; otros campos de cálculo (aFavor, etc.)
// se usan solo internamente y se omiten en el payload final.
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
    totalACobrar: 0,
    metodoPagoId: "",
    observaciones: "",
    matriculaRemoved: false,
    mensualidadId: "",
    periodoMensual: getMesVigente(),
};

const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria"),
});

// Componente que actualiza los valores de Formik sin reinicializar
const FormValuesUpdater: React.FC<{ newValues: Partial<CobranzasFormValues> }> = ({ newValues }) => {
    const { values, setValues } = useFormikContext<CobranzasFormValues>();
    useEffect(() => {
        setValues({ ...values, ...newValues });
    }, [newValues, setValues]);
    return null;
};

// Componente que calcula visualmente los totales (totalACobrar y totalCobrado)
const TotalsUpdater: React.FC<{ metodosPago: MetodoPagoResponse[] }> = ({ metodosPago }) => {
    const { values, setFieldValue } = useFormikContext<CobranzasFormValues>();

    useEffect(() => {
        // Calcula totalACobrar como suma de "importe" de cada detalle + recargo si método es DEBITO
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
        // Calcula totalCobrado sumando el valor de "aCobrar" de cada detalle
        const newTotalCobrado = values.detallePagos.reduce(
            (total, item) => total + Number(item.aCobrar || 0),
            0
        );
        setFieldValue("totalACobrar", newTotalACobrar);
        setFieldValue("totalCobrado", newTotalCobrado);
    }, [values.detallePagos, values.metodoPagoId, metodosPago, setFieldValue]);

    return null;
};

const CobranzasForm: React.FC = () => {
    const [initialValues, setInitialValues] = useState<CobranzasFormValues>(defaultValues);
    const [matricula, setMatricula] = useState<MatriculaResponse | null>(null);
    const [mensualidades, setMensualidades] = useState<ReporteMensualidadDTO[]>([]);
    const [inscripciones, setInscripciones] = useState<any[]>([]);
    const [allDisciplinas, setAllDisciplinas] = useState<DisciplinaDetalleResponse[]>([]);
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { alumnos, stocks, metodosPago, conceptos } = useCobranzasData();
    const { loadDeudasForAlumno } = useAlumnoDeudas();
    const [selectedAlumnoId, setSelectedAlumnoId] = useState<number | null>(null);
    const ultimoPago = useUltimoPago(selectedAlumnoId);
    const { loadMensualidadesAlumno } = useCargaMensualidades(alumnos);

    // Función para cargar matrícula
    const loadMatricula = async (alumnoId: number) => {
        const response = await matriculasApi.obtenerMatricula(alumnoId);
        setMatricula(response);
        return response;
    };

    // Función para cargar inscripciones del alumno
    const loadInscripciones = async (
        alumnoId: number,
        setFieldValue: (field: string, value: any, shouldValidate?: boolean) => Promise<void>
    ) => {
        const inscripcionesApi = await import("../../api/inscripcionesApi");
        let inscripcionesActivas: any[] = [];
        try {
            inscripcionesActivas = await inscripcionesApi.default.obtenerInscripcionesActivas(alumnoId);
        } catch (error) {
            toast.error("Error al cargar inscripciones:");
            inscripcionesActivas = [];
        }
        setInscripciones(inscripcionesActivas);
        await setFieldValue(
            "inscripcionId",
            inscripcionesActivas.length > 0 ? inscripcionesActivas[0].id : ""
        );
        return inscripcionesActivas;
    };

    // Manejo del cambio de alumno
    const handleAlumnoChange = useCallback(
        async (
            alumnoId: string,
            _currentValues: CobranzasFormValues,
            setFieldValue: (field: string, value: any, shouldValidate?: boolean) => Promise<void>
        ) => {
            const numAlumnoId = Number(alumnoId);
            setInscripciones([]);
            setAllDisciplinas([]);
            await setFieldValue("alumno", alumnoId);
            setSelectedAlumnoId(numAlumnoId);
            await setFieldValue("matriculaRemoved", false);
            try {
                const inscripcionesActivas = await loadInscripciones(numAlumnoId, setFieldValue);
                if (inscripcionesActivas.length === 0) {
                    const disciplinas = await disciplinasApi.listarDisciplinas();
                    setAllDisciplinas(disciplinas);
                }
                await loadMatricula(numAlumnoId);
                const mensualidadesRaw = await loadMensualidadesAlumno(numAlumnoId);
                setMensualidades(mensualidadesRaw);
                loadDeudasForAlumno(numAlumnoId, setFieldValue);
            } catch (err) {
                toast.error("Error al cargar datos del alumno:");
            }
        },
        [loadDeudasForAlumno, loadMensualidadesAlumno]
    );

    // Función para generar periodos (ej.: "ENERO 2025")
    const generatePeriodos = (numMeses = 12) => {
        const periodos = [];
        const currentDate = new Date(
            new Date().toLocaleString("en-US", { timeZone: "America/Argentina/Buenos_Aires" })
        );
        for (let i = 0; i < numMeses; i++) {
            const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - 2 + i, 1);
            const periodo = date
                .toLocaleString("default", { month: "long", year: "numeric" })
                .toUpperCase();
            periodos.push(periodo);
        }
        return periodos;
    };

    // Cargar datos iniciales si hay "ultimoPago" o se recibe un id en la URL
    useEffect(() => {
        if (!searchParams.get("id") && selectedAlumnoId && ultimoPago) {
            setInitialValues((prev) => ({
                ...prev,
                id: ultimoPago.id,
                reciboNro: ultimoPago.id.toString(),
                alumno: ultimoPago.alumnoId ? ultimoPago.alumnoId.toString() : "",
                inscripcionId: ultimoPago.inscripcionId.toString(),
                fecha: ultimoPago.fecha || new Date().toISOString().split("T")[0],
                // Se conservan "importe", "aCobrar" y se agrega "aFavor" para cumplir con el type
                detallePagos: ultimoPago.detallePagos.map((detalle: any) => ({
                    id: detalle.id,
                    codigoConcepto: detalle.codigoConcepto,
                    concepto: detalle.concepto,
                    cuota: detalle.cuota,
                    valorBase: detalle.valorBase,
                    bonificacionId: detalle.bonificacion ? detalle.bonificacion.id.toString() : "",
                    importe: detalle.importe,
                    aCobrar: detalle.aCobrar != null ? detalle.aCobrar : detalle.importe,
                    aFavor: detalle.aFavor != null ? detalle.aFavor : 0,  // Agregado para cumplir con la interfaz
                    autoGenerated: true,
                })),
                totalCobrado: ultimoPago.detallePagos.reduce(
                    (sum: number, detalle: any) =>
                        sum + (Number(detalle.aCobrar) || Number(detalle.importe) || 0),
                    0
                ),
                totalACobrar: ultimoPago.detallePagos.reduce(
                    (sum: number, detalle: any) => sum + (Number(detalle.importe) || 0),
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

    useEffect(() => {
        const idParam = searchParams.get("id");
        if (idParam) {
            pagosApi
                .obtenerPagoPorId(Number(idParam))
                .then((pagoData) => {
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
                            bonificacionId: detalle.bonificacion ? detalle.bonificacion.id.toString() : "",
                            importe: detalle.importe,
                            aCobrar: detalle.aCobrar,
                            aFavor: detalle.aFavor != null ? detalle.aFavor : 0, // Agregado
                            autoGenerated: true,
                        })),
                        totalCobrado: pagoData.detallePagos.reduce(
                            (sum: number, detalle: any) =>
                                sum + (Number(detalle.aCobrar) || Number(detalle.importe) || 0),
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
                    }));
                })
                .catch((err) => {
                    toast.error("Error al cargar los datos del pago.", err);
                });
        }
    }, [searchParams]);

    // onSubmit arma el payload final (PagoRegistroRequest)
    const onSubmit = async (values: CobranzasFormValues, actions: any) => {
        try {
            // Filtrar los detalles para eliminar aquellos de solo visualización (por ejemplo, con importe 0)
            const detallesFiltrados = values.detallePagos.filter(
                (detalle) => Number(detalle.importe) !== 0
            );
            // Armar el payload final: solo se envían los campos requeridos en cada detalle
            const pagoRegistroRequest: PagoRegistroRequest = {
                fecha: values.fecha,
                fechaVencimiento: values.fecha, // Ajusta si es necesario
                monto: Number(values.totalACobrar), // Convertir a number
                inscripcionId: Number(values.inscripcionId),
                metodoPagoId: values.metodoPagoId ? Number(values.metodoPagoId) : undefined,
                recargoAplicado: values.metodoPagoId
                    ? metodosPago.find(
                        (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
                    )?.descripcion.toUpperCase() === "DEBITO"
                    : false,
                bonificacionAplicada: false,
                detallePagos: detallesFiltrados.map((d) => ({
                    codigoConcepto: d.codigoConcepto ? String(d.codigoConcepto) : undefined, // Convertir a string si es number
                    concepto: d.concepto,
                    cuota: d.cuota,
                    valorBase: d.valorBase,
                    bonificacionId: d.bonificacionId ? Number(d.bonificacionId) : undefined,
                    recargoId: d.recargoId ? Number(d.recargoId) : undefined,
                    aCobrar: d.aCobrar,
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
            <Formik initialValues={initialValues} validationSchema={validationSchema} onSubmit={onSubmit}>
                {({ values, setFieldValue }) => {
                    // Opcional: aquí podrías agregar un useEffect para actualizar "importe" si se desea,
                    // pero se delega el cálculo final al backend.
                    return (
                        <Form className="w-full">
                            <FormValuesUpdater newValues={initialValues} />
                            <TotalsUpdater metodosPago={metodosPago} />
                            <AutoAddDetalles matricula={matricula} mensualidades={mensualidades} conceptos={conceptos} />
                            {/* Datos de Cobranza */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Cobranza</h2>
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
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
                                                handleAlumnoChange(
                                                    e.target.value,
                                                    values,
                                                    setFieldValue as (field: string, value: any, shouldValidate?: boolean) => Promise<void>
                                                )
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
                            {/* Datos de Disciplina y Conceptos */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Disciplina y Conceptos</h2>
                                <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
                                    <div className="sm:col-span-2">
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
                                            {inscripciones.length > 0
                                                ? inscripciones.map((insc) => (
                                                    <option key={insc.id} value={insc.disciplina.id}>
                                                        {insc.disciplina.nombre}
                                                    </option>
                                                ))
                                                : allDisciplinas.map((disc: DisciplinaDetalleResponse) => (
                                                    <option key={disc.id} value={disc.id}>
                                                        {disc.nombre}
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
                                        <Field name="cantidad" type="number" className="border p-2 w-full" min="1" />
                                    </div>
                                </div>
                            </div>
                            {/* Botón para agregar detalle manual */}
                            <div className="mb-4">
                                <button
                                    type="button"
                                    className="bg-green-500 text-white p-2 rounded mt-4"
                                    onClick={() => {
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
                                                        // Se asignan "importe" y "aCobrar" con el valor base para edición visual
                                                        importe: selectedConcept.precio,
                                                        aCobrar: selectedConcept.precio,
                                                        autoGenerated: true,
                                                        aFavor: 0
                                                    });
                                                    added = true;
                                                }
                                            }
                                        }
                                        if (values.disciplina && values.tarifa) {
                                            const inscripcionSeleccionada = inscripciones.find(
                                                (insc) => insc.disciplina.id.toString() === values.disciplina
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
                                                const conceptoDetalle = `${inscripcionSeleccionada.disciplina.nombre} - ${tarifaLabel} - ${values.periodoMensual}`;
                                                if (isDuplicate(conceptoDetalle)) {
                                                    toast.error("Concepto ya se encuentra agregado");
                                                } else {
                                                    newDetails.push({
                                                        id: null,
                                                        codigoConcepto: inscripcionSeleccionada.disciplina.id,
                                                        concepto: conceptoDetalle,
                                                        cuota: cantidad.toString(),
                                                        valorBase: total,
                                                        bonificacionId: "",
                                                        recargoId: "",
                                                        importe: total,
                                                        aCobrar: total,
                                                        autoGenerated: true,
                                                        aFavor: 0
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
                                                        autoGenerated: true,
                                                        aFavor: 0
                                                    });
                                                    added = true;
                                                }
                                            }
                                        }
                                        if (added) {
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
                                        } else if (!values.conceptoSeleccionado && !values.disciplina && !values.stockSeleccionado) {
                                            toast.error("Seleccione al menos un conjunto de campos para agregar");
                                        }
                                    }}
                                >
                                    Agregar Detalle
                                </button>
                            </div>
                            {/* Detalle de Facturación */}
                            <FieldArray name="detallePagos">
                                {({ remove }) => (
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
                                                {values.detallePagos && values.detallePagos.length > 0 ? (
                                                    values.detallePagos.map((_detalle, index) => (
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
                                                            {/* Campo editable para Importe */}
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
                                                                                // Opcional: actualizar aCobrar visualmente
                                                                                form.setFieldValue(`detallePagos.${index}.aCobrar`, newImporte);
                                                                                form.setFieldValue(`detallePagos.${index}.autoGenerated`, false);
                                                                            }}
                                                                        />
                                                                    )}
                                                                </Field>
                                                            </td>
                                                            {/* Campo editable para A Cobrar */}
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
                                                                    onClick={() => remove(index)}
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
                            {/* Totales y Pago */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Totales y Pago</h2>
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                                    <div>
                                        <label className="block font-medium">Total a Cobrar:</label>
                                        <input type="number" readOnly className="border p-2 w-full" value={values.totalACobrar} />
                                        {values.metodoPagoId &&
                                            (() => {
                                                const selectedMetodo = metodosPago.find(
                                                    (mp: MetodoPagoResponse) => mp.id.toString() === values.metodoPagoId
                                                );
                                                const isDebit = selectedMetodo && selectedMetodo.descripcion.toUpperCase() === "DEBITO";
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
        </ResponsiveContainer>
    );
};

export default CobranzasForm;
