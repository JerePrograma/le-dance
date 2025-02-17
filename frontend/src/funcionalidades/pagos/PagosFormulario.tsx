import React, { useEffect, useState, useCallback } from "react";
import { Formik, Form, Field, FieldArray, FormikHelpers } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import axios from "axios";
import pagosApi from "../../api/pagosApi";

// Importar los tipos definidos en tu sistema
import {
    AlumnoListadoResponse,
    DisciplinaListadoResponse,
    StockResponse,
    PagoRegistroRequest,
    // Se omiten DetallePagoRegistroRequest y PagoMedioRegistroRequest, pues se usan los tipos en línea
} from "../../types/types";

// Definición de los valores del formulario
interface CobranzasFormValues {
    reciboNro: string;
    alumno: string;
    inscripcionId: string;
    fecha: string;
    detalles: Array<{
        codigoConcepto?: string;
        concepto: string;
        cuota?: string;
        valorBase: number;
        bonificacion: number;
        recargo: number;
        aFavor: number;
        importe?: number;
        aCobrar?: number;
    }>;
    // Campos para agregar conceptos
    disciplina: string;
    conceptoSeleccionado: string;
    cantidad: number;
    // Totales y pagos
    aFavor: number;
    totalCobrado: number;
    pagos: Array<{
        metodoPagoId: number;
        monto: number;
    }>;
    observaciones: string;
}

const initialValues: CobranzasFormValues = {
    reciboNro: "AUTO-001",
    alumno: "",
    inscripcionId: "",
    fecha: new Date().toISOString().split("T")[0],
    detalles: [],
    disciplina: "",
    conceptoSeleccionado: "",
    cantidad: 1,
    aFavor: 0,
    totalCobrado: 0,
    pagos: [],
    observaciones: "",
};

// Validación básica con Yup
const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria"),
});

const CobranzasForm: React.FC = () => {
    // Estados para cargar dinámicamente los datos
    const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
    const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
    const [stocks, setStocks] = useState<StockResponse[]>([]);
    const [metodosPago, setMetodosPago] = useState<Array<{ id: number; nombre: string }>>([]);

    // Cargar datos al montar el componente
    useEffect(() => {
        axios
            .get("/api/alumnos")
            .then((res) => {
                const data = res.data;
                if (Array.isArray(data)) setAlumnos(data);
                else if (data.content && Array.isArray(data.content)) setAlumnos(data.content);
                else setAlumnos([]);
            })
            .catch((err) => console.error("Error al cargar alumnos:", err));

        pagosApi
            .listarDisciplinasBasicas()
            .then(setDisciplinas)
            .catch((err) => console.error("Error al cargar disciplinas:", err));

        pagosApi
            .listarStocksBasicos()
            .then(setStocks)
            .catch((err) => console.error("Error al cargar stocks:", err));

        pagosApi
            .listarAlumnosBasicos()
            .then(setAlumnos)
            .catch((err) => console.error("Error al cargar alumnos (básicos):", err));

        axios
            .get("/api/metodos-pago")
            .then((res) => setMetodosPago(res.data))
            .catch((err) => {
                console.error("Error al cargar métodos de pago:", err);
                setMetodosPago([
                    { id: 1, nombre: "Efectivo" },
                    { id: 2, nombre: "Tarjeta" },
                    { id: 3, nombre: "Cuotas" },
                ]);
            });
    }, []);

    // Al seleccionar un alumno, cargar sus disciplinas (y opcionalmente inscripciones)
    const handleAlumnoChange = useCallback(
        (alumnoId: string, setFieldValue: (field: string, value: any) => void) => {
            setFieldValue("alumno", alumnoId);
            axios
                .get(`/api/inscripciones?alumnoId=${alumnoId}`)
                .then(() => {
                    // Se podrían utilizar las inscripciones, si es necesario
                })
                .catch((err) => console.error("Error al cargar inscripciones:", err));
            axios
                .get(`/api/alumnos/${alumnoId}/disciplinas`)
                .then((res) => {
                    const data = res.data;
                    if (Array.isArray(data)) {
                        setDisciplinas(data);
                    } else if (data.content && Array.isArray(data.content)) {
                        setDisciplinas(data.content);
                    } else {
                        setDisciplinas([]);
                    }
                })
                .catch((err) => console.error("Error al cargar disciplinas:", err));
        },
        []
    );

    // Función para calcular el total a pagar sumando los importes de cada detalle
    const calculateTotalAPagar = (detalles: CobranzasFormValues["detalles"]) =>
        detalles.reduce((total, item) => total + Number(item.importe || 0), 0);

    // Actualiza el importe y el monto a cobrar de un detalle basado en sus valores
    const actualizarDetalleImporte = (detalle: CobranzasFormValues["detalles"][0]) => {
        const valorBase = Number(detalle.valorBase) || 0;
        const bonificacion = Number(detalle.bonificacion) || 0;
        const recargo = Number(detalle.recargo) || 0;
        const aFavor = Number(detalle.aFavor) || 0;
        const importe = valorBase - bonificacion + recargo - aFavor;
        return { ...detalle, importe, aCobrar: importe };
    };

    // Función de envío del formulario
    const onSubmit = async (
        values: CobranzasFormValues,
        actions: FormikHelpers<CobranzasFormValues>
    ) => {
        try {
            const detallesConImporte = values.detalles.map(actualizarDetalleImporte);
            const pagoRegistroRequest: PagoRegistroRequest = {
                fecha: values.fecha,
                fechaVencimiento: values.fecha,
                monto: calculateTotalAPagar(detallesConImporte),
                inscripcionId: Number(values.inscripcionId),
                metodoPagoId: undefined,
                recargoAplicado: false,
                bonificacionAplicada: 0,
                saldoRestante: calculateTotalAPagar(detallesConImporte),
                saldoAFavor: values.aFavor,
                detallePagos: detallesConImporte,
                pagoMedios: values.pagos,
            };
            await pagosApi.registrarPago(pagoRegistroRequest);
            toast.success("Cobranza registrada correctamente");
            actions.resetForm();
        } catch (error) {
            console.error("Error al registrar la cobranza:", error);
            toast.error("Error al registrar la cobranza");
        }
    };

    return (
        <div className="page-container p-4">
            <h1 className="page-title text-2xl font-bold mb-4">Gestión de Cobranzas</h1>
            <Formik initialValues={initialValues} validationSchema={validationSchema} onSubmit={onSubmit}>
                {({ values, setFieldValue }) => (
                    <Form className="max-w-5xl mx-auto">
                        {/* ─── DATOS GENERALES ────────────────────────────── */}
                        <div className="border p-4 mb-4">
                            <h2 className="font-bold mb-2">Datos de Cobranza</h2>
                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block font-medium">Recibo Nro:</label>
                                    <Field name="reciboNro" readOnly className="border p-2 w-full bg-gray-100" />
                                </div>
                                <div>
                                    <label className="block font-medium">Alumno:</label>
                                    <Field
                                        as="select"
                                        name="alumno"
                                        className="border p-2 w-full"
                                        onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                                            handleAlumnoChange(e.target.value, setFieldValue)
                                        }
                                    >
                                        <option value="">Seleccione un alumno</option>
                                        {alumnos.map((alumno) => (
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

                        {/* ─── DETALLES DE FACTURACIÓN ─────────────────────── */}
                        <div className="border p-4 mb-4">
                            <h2 className="font-bold mb-2">Detalles de Facturación</h2>
                            <FieldArray name="detalles">
                                {({ push, remove }) => (
                                    <>
                                        <table className="min-w-full border mb-4">
                                            <thead>
                                                <tr>
                                                    <th className="border p-2">Código (ID)</th>
                                                    <th className="border p-2">Concepto</th>
                                                    <th className="border p-2">Cuota/Cantidad</th>
                                                    <th className="border p-2">Valor Base</th>
                                                    <th className="border p-2">Bonificación</th>
                                                    <th className="border p-2">Recargo</th>
                                                    <th className="border p-2">A Favor</th>
                                                    <th className="border p-2">Importe</th>
                                                    <th className="border p-2">A Cobrar</th>
                                                    <th className="border p-2">Acciones</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {values.detalles && values.detalles.length > 0 ? (
                                                    values.detalles.map((detalle, index) => {
                                                        const valorBase = Number(detalle.valorBase) || 0;
                                                        const bonificacion = Number(detalle.bonificacion) || 0;
                                                        const recargo = Number(detalle.recargo) || 0;
                                                        const aFavor = Number(detalle.aFavor) || 0;
                                                        const aCobrar = valorBase - bonificacion + recargo - aFavor;
                                                        return (
                                                            <tr key={index}>
                                                                <td className="border p-2">
                                                                    <Field name={`detalles.${index}.codigoConcepto`} type="text" className="w-full" />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field name={`detalles.${index}.concepto`} type="text" className="w-full" />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field name={`detalles.${index}.cuota`} type="text" className="w-full" />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.valorBase`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                            const newValorBase = Number(e.target.value);
                                                                            setFieldValue(`detalles.${index}.valorBase`, newValorBase);
                                                                            const newImporte = newValorBase;
                                                                            const newACobrar = newValorBase - bonificacion + recargo - aFavor;
                                                                            setFieldValue(`detalles.${index}.importe`, newImporte);
                                                                            setFieldValue(`detalles.${index}.aCobrar`, newACobrar);
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.bonificacion`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                            const newBonificacion = Number(e.target.value);
                                                                            setFieldValue(`detalles.${index}.bonificacion`, newBonificacion);
                                                                            const newACobrar = valorBase - newBonificacion + recargo - aFavor;
                                                                            setFieldValue(`detalles.${index}.aCobrar`, newACobrar);
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.recargo`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                            const newRecargo = Number(e.target.value);
                                                                            setFieldValue(`detalles.${index}.recargo`, newRecargo);
                                                                            const newACobrar = valorBase - bonificacion + newRecargo - aFavor;
                                                                            setFieldValue(`detalles.${index}.aCobrar`, newACobrar);
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.aFavor`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                            const newAFavor = Number(e.target.value);
                                                                            setFieldValue(`detalles.${index}.aFavor`, newAFavor);
                                                                            const newACobrar = valorBase - bonificacion + recargo - newAFavor;
                                                                            setFieldValue(`detalles.${index}.aCobrar`, newACobrar);
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field name={`detalles.${index}.importe`} type="number" className="w-full" readOnly />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field name={`detalles.${index}.aCobrar`} type="number" className="w-full" readOnly value={aCobrar} />
                                                                </td>
                                                                <td className="border p-2 text-center">
                                                                    <button
                                                                        type="button"
                                                                        className="bg-red-500 text-white p-1 rounded"
                                                                        onClick={() => remove(index)}
                                                                    >
                                                                        Eliminar
                                                                    </button>
                                                                </td>
                                                            </tr>
                                                        );
                                                    })
                                                ) : (
                                                    <tr>
                                                        <td colSpan={10} className="text-center p-2">
                                                            No hay conceptos agregados
                                                        </td>
                                                    </tr>
                                                )}
                                            </tbody>
                                        </table>

                                        {/* ─── AGREGAR CONCEPTOS ───────────────────── */}
                                        <div className="grid grid-cols-4 gap-4 items-end">
                                            {/* Selector de Disciplina */}
                                            <div>
                                                <label className="block font-medium">Disciplina:</label>
                                                <Field as="select" name="disciplina" className="border p-2 w-full">
                                                    <option value="">Seleccione una disciplina</option>
                                                    {disciplinas.map((disc) => (
                                                        <option key={disc.id} value={disc.nombre}>
                                                            {disc.nombre}
                                                        </option>
                                                    ))}
                                                </Field>
                                            </div>
                                            <div>
                                                <button
                                                    type="button"
                                                    className="bg-blue-500 text-white p-2 rounded"
                                                    onClick={() => {
                                                        if (values.disciplina) {
                                                            push({
                                                                codigoConcepto: `DISC-${values.disciplina}`,
                                                                concepto: values.disciplina,
                                                                cuota: "",
                                                                valorBase: 0,
                                                                bonificacion: 0,
                                                                recargo: 0,
                                                                aFavor: 0,
                                                                importe: 0,
                                                                aCobrar: 0,
                                                            });
                                                            setFieldValue("disciplina", "");
                                                        }
                                                    }}
                                                >
                                                    Grabar Disciplinas
                                                </button>
                                            </div>
                                            {/* Selector de Concepto (Stock) */}
                                            <div>
                                                <label className="block font-medium">Concepto (Stock):</label>
                                                <Field as="select" name="conceptoSeleccionado" className="border p-2 w-full">
                                                    <option value="">Seleccione un stock</option>
                                                    {stocks.map((prod) => (
                                                        <option key={prod.id} value={prod.id}>
                                                            {prod.nombre}
                                                        </option>
                                                    ))}
                                                </Field>
                                            </div>
                                            <div>
                                                <label className="block font-medium">Cantidad:</label>
                                                <Field name="cantidad" type="number" className="border p-2 w-full" min="1" />
                                            </div>
                                            <div className="col-span-2">
                                                <button
                                                    type="button"
                                                    className="bg-green-500 text-white p-2 rounded mt-4"
                                                    onClick={() => {
                                                        const selectedProductId = Number(values.conceptoSeleccionado);
                                                        const selectedProduct = stocks.find((p) => p.id === selectedProductId);
                                                        if (selectedProduct) {
                                                            const cantidad = Number(values.cantidad) || 1;
                                                            const valor = selectedProduct.precio * cantidad;
                                                            push({
                                                                codigoConcepto: selectedProduct.id.toString(),
                                                                concepto: selectedProduct.nombre,
                                                                cuota: cantidad.toString(),
                                                                valorBase: valor,
                                                                bonificacion: 0,
                                                                recargo: 0,
                                                                aFavor: 0,
                                                                importe: valor,
                                                                aCobrar: valor,
                                                            });
                                                            setFieldValue("conceptoSeleccionado", "");
                                                            setFieldValue("cantidad", 1);
                                                        } else {
                                                            toast.error("Seleccione un stock válido");
                                                        }
                                                    }}
                                                >
                                                    Agregar
                                                </button>
                                            </div>
                                            <div className="col-span-2">
                                                <button
                                                    type="button"
                                                    className="bg-yellow-500 text-white p-2 rounded mt-4"
                                                    onClick={() => {
                                                        // Elimina el primer concepto que corresponda a una disciplina
                                                        const indexToRemove = values.detalles.findIndex((item) =>
                                                            String(item.codigoConcepto).startsWith("DISC-")
                                                        );
                                                        if (indexToRemove !== -1) {
                                                            remove(indexToRemove);
                                                        } else {
                                                            toast.info("No hay disciplinas para borrar");
                                                        }
                                                    }}
                                                >
                                                    Borrar Dis
                                                </button>
                                            </div>
                                        </div>
                                    </>
                                )}
                            </FieldArray>
                        </div>

                        {/* ─── TOTALES Y PAGOS ───────────────────────────── */}
                        <div className="border p-4 mb-4">
                            <h2 className="font-bold mb-2">Totales y Pagos</h2>
                            <div className="grid grid-cols-3 gap-4">
                                <div>
                                    <label className="block font-medium">Total a Pagar:</label>
                                    <input
                                        type="number"
                                        readOnly
                                        className="border p-2 w-full bg-gray-100"
                                        value={calculateTotalAPagar(values.detalles)}
                                    />
                                </div>
                                <div>
                                    <label className="block font-medium">A Favor:</label>
                                    <Field name="aFavor" type="number" className="border p-2 w-full" readOnly />
                                </div>
                                <div>
                                    <label className="block font-medium">Total Cobrado:</label>
                                    <Field name="totalCobrado" type="number" className="border p-2 w-full" readOnly />
                                </div>
                            </div>
                            <FieldArray name="pagos">
                                {({ push, remove }) => (
                                    <div className="mt-4">
                                        <h3 className="font-medium mb-2">Métodos de Pago</h3>
                                        {values.pagos && values.pagos.length > 0 ? (
                                            values.pagos.map((_pago, index) => (
                                                <div key={index} className="grid grid-cols-3 gap-4 items-end mb-2">
                                                    <div>
                                                        <label className="block font-medium">Método de Pago:</label>
                                                        <Field as="select" name={`pagos.${index}.metodoPagoId`} className="border p-2 w-full">
                                                            <option value="">Seleccione</option>
                                                            {metodosPago.map((mp) => (
                                                                <option key={mp.id} value={mp.id}>
                                                                    {mp.nombre}
                                                                </option>
                                                            ))}
                                                        </Field>
                                                    </div>
                                                    <div>
                                                        <label className="block font-medium">Monto:</label>
                                                        <Field name={`pagos.${index}.monto`} type="number" className="border p-2 w-full" />
                                                    </div>
                                                    <div>
                                                        <button
                                                            type="button"
                                                            className="bg-red-500 text-white p-2 rounded"
                                                            onClick={() => remove(index)}
                                                        >
                                                            Eliminar
                                                        </button>
                                                    </div>
                                                </div>
                                            ))
                                        ) : (
                                            <p>No se han agregado métodos de pago.</p>
                                        )}
                                        <button
                                            type="button"
                                            className="bg-blue-500 text-white p-2 rounded mt-2"
                                            onClick={() => push({ metodoPagoId: 0, monto: 0 })}
                                        >
                                            Agregar Método de Pago
                                        </button>
                                    </div>
                                )}
                            </FieldArray>
                        </div>

                        {/* ─── OBSERVACIONES ───────────────────────────── */}
                        <div className="border p-4 mb-4">
                            <label className="block font-medium">Observaciones:</label>
                            <Field as="textarea" name="observaciones" className="border p-2 w-full" rows="3" />
                        </div>

                        {/* ─── BOTONES DE ACCIÓN ────────────────────────── */}
                        <div className="flex justify-end gap-4">
                            <button type="submit" className="bg-green-500 text-white p-2 rounded">
                                Registrar Cobranza
                            </button>
                            <button type="reset" className="bg-gray-500 text-white p-2 rounded">
                                Cancelar
                            </button>
                        </div>
                    </Form>
                )}
            </Formik>
        </div>
    );
};

export default CobranzasForm;
