import React, { useEffect, useState, useCallback } from "react";
import { Formik, Form, Field, FieldArray, FormikHelpers } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import pagosApi from "../../api/pagosApi";
import conceptosApi from "../../api/conceptosApi";
import alumnosApi from "../../api/alumnosApi";
import matriculasApi from "../../api/matriculasApi";
import inscripcionesApi from "../../api/inscripcionesApi";
import metodosPagoApi from "../../api/metodosPagoApi";
import { MatriculaAutoAdd } from "../../hooks/context/useFormikContext";

// Importar los tipos definidos en tu sistema
import {
    AlumnoListadoResponse,
    DisciplinaListadoResponse,
    StockResponse,
    PagoRegistroRequest,
    ConceptoResponse,
    MatriculaResponse,
    CobranzasFormValues,
    MetodoPagoResponse,
} from "../../types/types";

const initialValues: CobranzasFormValues = {
    reciboNro: "AUTO-001",
    alumno: "",
    inscripcionId: "",
    fecha: new Date().toISOString().split("T")[0],
    detalles: [],
    disciplina: "",
    tarifa: "",
    cantidad: 1,
    // aFavor se maneja internamente y no se muestra, pero lo inicializamos en 0
    aFavor: 0,
    totalCobrado: 0,
    metodoPagoId: "",
    observaciones: "",
    conceptoSeleccionado: "",
    stockSeleccionado: "",
    matriculaRemoved: false,
};

const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria"),
});

const CobranzasForm: React.FC = () => {
    const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
    const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
    const [stocks, setStocks] = useState<StockResponse[]>([]);
    const [metodosPago, setMetodosPago] = useState<MetodoPagoResponse[]>([]);
    const [conceptos, setConceptos] = useState<ConceptoResponse[]>([]);
    const [matricula, setMatricula] = useState<MatriculaResponse | null>(null);

    // Cargar alumnos
    useEffect(() => {
        alumnosApi
            .listar()
            .then(setAlumnos)
            .catch((err) => console.error("Error al cargar alumnos:", err));
    }, []);

    // Cargar disciplinas, stocks y alumnos básicos
    useEffect(() => {
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
    }, []);

    // Cargar métodos de pago
    useEffect(() => {
        metodosPagoApi
            .listarMetodosPago()
            .then((data) => setMetodosPago(Array.isArray(data) ? data : []))
            .catch((err) => {
                console.error("Error al cargar métodos de pago:", err);
            });
    }, []);

    // Cargar conceptos
    useEffect(() => {
        conceptosApi
            .listarConceptos()
            .then(setConceptos)
            .catch((err) => console.error("Error al cargar conceptos:", err));
    }, []);

    // Al seleccionar un alumno, cargar disciplinas y la inscripción activa
    const handleAlumnoChange = useCallback(
        (alumnoId: string, setFieldValue: (field: string, value: any) => void) => {
            setFieldValue("alumno", alumnoId);
            // Reiniciamos el flag para el nuevo alumno
            setFieldValue("matriculaRemoved", false);
            alumnosApi
                .obtenerDisciplinas(Number(alumnoId))
                .then((data) => setDisciplinas(data))
                .catch((err) =>
                    console.error("Error al cargar disciplinas del alumno:", err)
                );
            inscripcionesApi
                .obtenerInscripcionActiva(Number(alumnoId))
                .then((inscripcion) => {
                    setFieldValue("inscripcionId", inscripcion.id);
                    return matriculasApi.obtenerMatricula(Number(alumnoId));
                })
                .then((matriculaResponse) => {
                    setMatricula(matriculaResponse);
                })
                .catch((err) =>
                    console.error("Error al obtener inscripción o matrícula:", err)
                );
        },
        []
    );

    // Función para sumar todos los importes de los detalles
    const calculateTotalAPagar = (detalles: CobranzasFormValues["detalles"]) =>
        detalles.reduce((total, item) => total + Number(item.importe || 0), 0);

    // Actualiza el importe y aCobrar para cada detalle
    const actualizarDetalleImporte = (detalle: CobranzasFormValues["detalles"][0]) => {
        const valorBase = Number(detalle.valorBase) || 0;
        const bonificacion = Number(detalle.bonificacion) || 0;
        const recargo = Number(detalle.recargo) || 0;
        // aFavor se mantiene como 0 en la vista (o se puede actualizar según lógica interna)
        const aFavor = Number(detalle.aFavor) || 0;
        const importe = valorBase - bonificacion + recargo - aFavor;
        return { ...detalle, importe, aCobrar: importe };
    };

    const onSubmit = async (
        values: CobranzasFormValues,
        actions: FormikHelpers<CobranzasFormValues>
    ) => {
        try {
            const detallesConImporte = values.detalles.map(actualizarDetalleImporte);
            const pagoMatricula = values.detalles.some((detalle) => {
                const codigo = detalle.codigoConcepto?.toLowerCase() || "";
                const concepto = detalle.concepto?.toLowerCase() || "";
                return (
                    codigo.includes("matricula") ||
                    concepto === "matricula" ||
                    concepto === "matrícula"
                );
            });

            // Verificamos si el método de pago seleccionado es DEBITO
            const selectedMetodo = metodosPago.find(
                (mp) => mp.id.toString() === values.metodoPagoId
            );
            const isDebit =
                selectedMetodo &&
                selectedMetodo.descripcion.toUpperCase() === "DEBITO";

            // Calculamos el total de los detalles y, si es débito, sumamos $5000
            const baseTotal = calculateTotalAPagar(detallesConImporte);
            const computedTotal = baseTotal + (isDebit ? 5000 : 0);

            // Si se seleccionó DEBITO, mostramos un aviso
            if (isDebit) {
                toast.info("Se ha agregado recargo de $5000 por método de pago DEBITO");
            }

            const pagoRegistroRequest: PagoRegistroRequest = {
                fecha: values.fecha,
                // La fecha de vencimiento se calculará en el backend; se envía la misma fecha
                fechaVencimiento: values.fecha,
                monto: computedTotal,
                inscripcionId: Number(values.inscripcionId),
                metodoPagoId: values.metodoPagoId ? Number(values.metodoPagoId) : undefined,
                recargoAplicado: isDebit,
                bonificacionAplicada: 0,
                saldoRestante: computedTotal,
                // aFavor se maneja internamente, no se expone en el formulario
                saldoAFavor: 0,
                detallePagos: detallesConImporte,
                pagoMedios: [],
                pagoMatricula: pagoMatricula,
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
            <Formik
                initialValues={initialValues}
                validationSchema={validationSchema}
                onSubmit={onSubmit}
            >
                {({ values, setFieldValue }) => {
                    // Determinar si el método de pago es DEBITO para actualizar el total a pagar en tiempo real
                    const selectedMetodo = metodosPago.find(
                        (mp) => mp.id.toString() === values.metodoPagoId
                    );
                    const isDebit =
                        selectedMetodo &&
                        selectedMetodo.descripcion.toUpperCase() === "DEBITO";
                    const baseTotal = calculateTotalAPagar(values.detalles);
                    const computedTotal = baseTotal + (isDebit ? 5000 : 0);

                    return (
                        <Form className="max-w-5xl mx-auto">
                            <MatriculaAutoAdd matricula={matricula} conceptos={conceptos} />

                            {/* DATOS GENERALES */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Cobranza</h2>
                                <div className="grid grid-cols-3 gap-4">
                                    <div>
                                        <label className="block font-medium">Recibo Nro:</label>
                                        <Field
                                            name="reciboNro"
                                            readOnly
                                            className="border p-2 w-full"
                                        />
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
                                        <Field
                                            name="fecha"
                                            type="date"
                                            className="border p-2 w-full"
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* DATOS DE DISCIPLINA Y CLASES */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Datos de Disciplina</h2>
                                <div className="grid grid-cols-4 gap-4 items-end">
                                    <div className="col-span-2">
                                        <label className="block font-medium">Disciplina:</label>
                                        <Field
                                            as="select"
                                            name="disciplina"
                                            className="border p-2 w-full"
                                            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                                                const selectedId = e.target.value;
                                                setFieldValue("disciplina", selectedId);
                                                setFieldValue("tarifa", "");
                                            }}
                                        >
                                            <option value="">Seleccione una disciplina</option>
                                            {disciplinas.map((disc) => (
                                                <option key={disc.id} value={disc.id}>
                                                    {disc.nombre}
                                                </option>
                                            ))}
                                        </Field>
                                    </div>
                                    <div>
                                        <label className="block font-medium">Tarifa:</label>
                                        <Field
                                            as="select"
                                            name="tarifa"
                                            className="border p-2 w-full"
                                        >
                                            <option value="">Seleccione una tarifa</option>
                                            <option value="CUOTA">CUOTA</option>
                                            <option value="CLASE_SUELTA">CLASE SUELTA</option>
                                            <option value="CLASE_PRUEBA">CLASE DE PRUEBA</option>
                                        </Field>
                                    </div>
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
                                                    {conceptos.map((conc) => (
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
                                                    {stocks.map((prod) => (
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
                                    {/* Botón para agregar detalle */}
                                    <div>
                                        <button
                                            type="button"
                                            className="bg-green-500 text-white p-2 rounded mt-4"
                                            onClick={() => {
                                                const newDetails = [...values.detalles];

                                                // Si se seleccionó un concepto:
                                                if (values.conceptoSeleccionado) {
                                                    const selectedConcept = conceptos.find(
                                                        (c) => c.id.toString() === values.conceptoSeleccionado
                                                    );
                                                    if (selectedConcept) {
                                                        const descLower = selectedConcept.descripcion
                                                            .toLowerCase()
                                                            .trim();
                                                        if (descLower === "matricula") {
                                                            // Evitar duplicados
                                                            const hasMatricula = newDetails.some(
                                                                (detail) =>
                                                                    detail.concepto?.toLowerCase().trim() === "matricula"
                                                            );
                                                            if (hasMatricula) {
                                                                toast.error("El concepto Matrícula ya ha sido agregado.");
                                                            } else if (matricula) {
                                                                newDetails.push({
                                                                    codigoConcepto: selectedConcept.id.toString(),
                                                                    concepto: selectedConcept.descripcion,
                                                                    cuota: "1",
                                                                    valorBase: selectedConcept.precio,
                                                                    bonificacion: 0,
                                                                    recargo: 0,
                                                                    aFavor: 0,
                                                                    importe: selectedConcept.precio,
                                                                    aCobrar: selectedConcept.precio,
                                                                });
                                                            }
                                                        } else {
                                                            const cantidad = Number(values.cantidad) || 1;
                                                            const valor = selectedConcept.precio * cantidad;
                                                            newDetails.push({
                                                                codigoConcepto: selectedConcept.id.toString(),
                                                                concepto: selectedConcept.descripcion,
                                                                cuota: cantidad.toString(),
                                                                valorBase: valor,
                                                                bonificacion: 0,
                                                                recargo: 0,
                                                                aFavor: 0,
                                                                importe: valor,
                                                                aCobrar: valor,
                                                            });
                                                        }
                                                    }
                                                }

                                                // Agregar detalle de disciplina si se seleccionó
                                                if (values.disciplina && values.tarifa) {
                                                    const selectedDisc = disciplinas.find(
                                                        (d) => d.id.toString() === values.disciplina
                                                    );
                                                    if (selectedDisc) {
                                                        let precio = 0;
                                                        let tarifaLabel = "";
                                                        if (values.tarifa === "CUOTA") {
                                                            precio = selectedDisc.valorCuota;
                                                            tarifaLabel = "CUOTA";
                                                        } else if (values.tarifa === "CLASE_SUELTA") {
                                                            precio = selectedDisc.claseSuelta || 0;
                                                            tarifaLabel = "CLASE SUELTA";
                                                        } else if (values.tarifa === "CLASE_PRUEBA") {
                                                            precio = selectedDisc.clasePrueba || 0;
                                                            tarifaLabel = "CLASE DE PRUEBA";
                                                        }
                                                        const cantidad = Number(values.cantidad) || 1;
                                                        const total = precio * cantidad;
                                                        newDetails.push({
                                                            codigoConcepto: selectedDisc.id.toString(),
                                                            concepto: `${selectedDisc.nombre.toUpperCase()} - ${tarifaLabel}`,
                                                            cuota: cantidad.toString(),
                                                            valorBase: total,
                                                            bonificacion: 0,
                                                            recargo: 0,
                                                            aFavor: 0,
                                                            importe: total,
                                                            aCobrar: total,
                                                        });
                                                    }
                                                }

                                                // Agregar detalle de stock si se seleccionó
                                                if (values.stockSeleccionado) {
                                                    const selectedStock = stocks.find(
                                                        (s) => s.id.toString() === values.stockSeleccionado
                                                    );
                                                    if (selectedStock) {
                                                        const cantidad = Number(values.cantidad) || 1;
                                                        const valor = selectedStock.precio * cantidad;
                                                        newDetails.push({
                                                            codigoConcepto: selectedStock.id.toString(),
                                                            concepto: selectedStock.nombre,
                                                            cuota: cantidad.toString(),
                                                            valorBase: valor,
                                                            bonificacion: 0,
                                                            recargo: 0,
                                                            aFavor: 0,
                                                            importe: valor,
                                                            aCobrar: valor,
                                                        });
                                                    }
                                                }

                                                if (newDetails.length > values.detalles.length) {
                                                    setFieldValue("detalles", newDetails);
                                                    // Reiniciamos campos de selección
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
                                            Agregar
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {/* DETALLES DE FACTURACIÓN */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Detalles de Facturación</h2>
                                <FieldArray name="detalles">
                                    {({ remove }) => (
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
                                                            const aCobrar = valorBase - bonificacion + recargo; // aFavor no se muestra
                                                            const handleFieldChange = (
                                                                field: string,
                                                                newValue: number,
                                                                currentValorBase: number = valorBase,
                                                                currentBonificacion: number = bonificacion,
                                                                currentRecargo: number = recargo
                                                            ) => {
                                                                setFieldValue(`detalles.${index}.${field}`, newValue);
                                                                const updatedValorBase = field === "valorBase" ? newValue : currentValorBase;
                                                                const updatedBonificacion = field === "bonificacion" ? newValue : currentBonificacion;
                                                                const updatedRecargo = field === "recargo" ? newValue : currentRecargo;
                                                                const updatedACobrar =
                                                                    updatedValorBase - updatedBonificacion + updatedRecargo;
                                                                setFieldValue(`detalles.${index}.aCobrar`, updatedACobrar);
                                                                setFieldValue(`detalles.${index}.importe`, updatedValorBase);
                                                            };

                                                            return (
                                                                <tr key={index}>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.codigoConcepto`}
                                                                            type="text"
                                                                            className="w-full"
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.concepto`}
                                                                            type="text"
                                                                            className="w-full"
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.cuota`}
                                                                            type="text"
                                                                            className="w-full"
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.valorBase`}
                                                                            type="number"
                                                                            className="w-full"
                                                                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                                const newValue = Number(e.target.value);
                                                                                handleFieldChange("valorBase", newValue);
                                                                            }}
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.bonificacion`}
                                                                            type="number"
                                                                            className="w-full"
                                                                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                                const newValue = Number(e.target.value);
                                                                                handleFieldChange("bonificacion", newValue);
                                                                            }}
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.recargo`}
                                                                            type="number"
                                                                            className="w-full"
                                                                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                                                                                const newValue = Number(e.target.value);
                                                                                handleFieldChange("recargo", newValue);
                                                                            }}
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.importe`}
                                                                            type="number"
                                                                            className="w-full"
                                                                            readOnly
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2">
                                                                        <Field
                                                                            name={`detalles.${index}.aCobrar`}
                                                                            type="number"
                                                                            className="w-full"
                                                                            readOnly
                                                                            value={aCobrar}
                                                                        />
                                                                    </td>
                                                                    <td className="border p-2 text-center">
                                                                        <button
                                                                            type="button"
                                                                            className="bg-red-500 text-white p-1 rounded"
                                                                            onClick={() => {
                                                                                remove(index);
                                                                            }}
                                                                        >
                                                                            Eliminar
                                                                        </button>
                                                                    </td>
                                                                </tr>
                                                            );
                                                        })
                                                    ) : (
                                                        <tr>
                                                            <td colSpan={9} className="text-center p-2">
                                                                No hay conceptos agregados
                                                            </td>
                                                        </tr>
                                                    )}
                                                </tbody>
                                            </table>
                                        </>
                                    )}
                                </FieldArray>
                            </div>

                            {/* TOTALES Y PAGO */}
                            <div className="border p-4 mb-4">
                                <h2 className="font-bold mb-2">Totales y Pago</h2>
                                <div className="grid grid-cols-3 gap-4">
                                    <div>
                                        <label className="block font-medium">Total a Pagar:</label>
                                        <input
                                            type="number"
                                            readOnly
                                            className="border p-2 w-full"
                                            value={computedTotal}
                                        />
                                        {isDebit && (
                                            <p className="text-sm text-info">
                                                Se ha agregado recargo de $5000 por DEBITO.
                                            </p>
                                        )}
                                    </div>
                                    <div>
                                        <label className="block font-medium">Total Cobrado:</label>
                                        <input
                                            type="number"
                                            readOnly
                                            className="border p-2 w-full "
                                            value={computedTotal}
                                        />
                                    </div>
                                    <div>
                                        <label className="block font-medium">Método de Pago:</label>
                                        <Field as="select" name="metodoPagoId" className="border p-2 w-full">
                                            <option value="">Seleccione un método de pago</option>
                                            {metodosPago.map((mp) => (
                                                <option key={mp.id} value={mp.id}>
                                                    {mp.descripcion}
                                                </option>
                                            ))}
                                        </Field>
                                    </div>
                                </div>
                            </div>

                            {/* OBSERVACIONES */}
                            <div className="border p-4 mb-4">
                                <label className="block font-medium">Observaciones:</label>
                                <Field
                                    as="textarea"
                                    name="observaciones"
                                    className="border p-2 w-full"
                                    rows="3"
                                />
                            </div>

                            {/* BOTONES DE ACCIÓN */}
                            <div className="flex justify-end gap-4">
                                <button type="submit" className="bg-green-500 text-white p-2 rounded">
                                    Registrar Cobranza
                                </button>
                                <button type="reset" className="bg-gray-500 text-white p-2 rounded">
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
