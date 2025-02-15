import { Formik, Form, Field, FieldArray } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";

// Valores iniciales del formulario
const initialValues = {
    reciboNro: "AUTO-001", // Valor autogenerado (podría actualizarse dinámicamente)
    alumno: "", // Aquí se podría implementar un autocomplete para buscar por ID/nombre
    fecha: new Date().toISOString().split("T")[0],
    detalles: [], // Array de conceptos agregados a la factura
    // Campos para agregar conceptos
    disciplina: "",
    conceptoSeleccionado: "",
    cantidad: 1,
    // Totales y Pagos
    aFavor: 0, // Valor obtenido del alumno (ej: alumno.aFavor)
    totalCobrado: 0,
    pagos: [], // Array para métodos de pago
    // Observaciones
    observaciones: ""
};

// Validaciones básicas (se pueden ampliar según se requiera)
const validationSchema = Yup.object().shape({
    alumno: Yup.string().required("El alumno es obligatorio"),
    fecha: Yup.string().required("La fecha es obligatoria")
});

// Opciones de ejemplo para disciplinas, productos y métodos de pago
const disciplinasOptions = [
    { id: 1, nombre: "Fútbol" },
    { id: 2, nombre: "Básquet" },
    { id: 3, nombre: "Natación" }
];

const productosOptions = [
    { id: 33, nombre: "MATRÍCULA 2024", valor: 15000 },
    { id: 7, nombre: "Ballet Intermedio/Avanz.", valor: 26000 },
    { id: 8, nombre: "MALLA", valor: 28000 }
];

const metodosPagoOptions = [
    { id: 1, nombre: "Efectivo" },
    { id: 2, nombre: "Tarjeta" },
    { id: 3, nombre: "Cuotas" }
];

const CobranzasForm = () => {
    // Función para calcular el total a pagar sumando los importes de cada concepto
    const calculateTotalAPagar = (detalles) =>
        detalles.reduce((total, item) => total + Number(item.importe || 0), 0);

    return (
        <div className="page-container p-4">
            <h1 className="page-title text-2xl font-bold mb-4">
                Gestión de Cobranzas
            </h1>
            <Formik
                initialValues={initialValues}
                validationSchema={validationSchema}
                onSubmit={(values) => {
                    console.log("Valores del formulario:", values);
                    toast.success("Cobranza registrada correctamente");
                    // Aquí se podría invocar una API para registrar el pago
                }}
            >
                {({ values, setFieldValue }) => (
                    <Form className="max-w-5xl mx-auto">
                        {/* ─── DATOS GENERALES ────────────────────────────── */}
                        <div className="border p-4 mb-4">
                            <h2 className="font-bold mb-2">Datos de Cobranza</h2>
                            <div className="grid grid-cols-3 gap-4">
                                {/* Recibo Nro (solo lectura) */}
                                <div>
                                    <label className="block font-medium">Recibo Nro:</label>
                                    <Field
                                        name="reciboNro"
                                        readOnly
                                        className="border p-2 w-full bg-gray-100"
                                    />
                                </div>
                                {/* Alumno: búsqueda por ID o nombre */}
                                <div>
                                    <label className="block font-medium">Alumno:</label>
                                    <Field
                                        name="alumno"
                                        type="text"
                                        placeholder="Buscar por ID o nombre"
                                        className="border p-2 w-full"
                                    />
                                    {/* Aquí se podría integrar un componente de autocompletado */}
                                </div>
                                {/* Fecha */}
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
                                                    <th className="border p-2">Valor</th>
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
                                                        // Cálculo automático de "A Cobrar"
                                                        const valor = Number(detalle.valor) || 0;
                                                        const bonificacion = Number(detalle.bonificacion) || 0;
                                                        const recargo = Number(detalle.recargo) || 0;
                                                        const aFavor = Number(detalle.aFavor) || 0;
                                                        const aCobrar = valor - bonificacion + recargo - aFavor;

                                                        return (
                                                            <tr key={index}>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.codigo`}
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
                                                                        name={`detalles.${index}.valor`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e) => {
                                                                            const newValor = Number(e.target.value);
                                                                            setFieldValue(
                                                                                `detalles.${index}.valor`,
                                                                                newValor
                                                                            );
                                                                            // Recalcular importe y aCobrar
                                                                            const newImporte = newValor; // Se asume importe = valor
                                                                            const newACobrar =
                                                                                newValor -
                                                                                bonificacion +
                                                                                recargo -
                                                                                aFavor;
                                                                            setFieldValue(
                                                                                `detalles.${index}.importe`,
                                                                                newImporte
                                                                            );
                                                                            setFieldValue(
                                                                                `detalles.${index}.aCobrar`,
                                                                                newACobrar
                                                                            );
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.bonificacion`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e) => {
                                                                            const newBonificacion = Number(e.target.value);
                                                                            setFieldValue(
                                                                                `detalles.${index}.bonificacion`,
                                                                                newBonificacion
                                                                            );
                                                                            const newACobrar =
                                                                                valor - newBonificacion + recargo - aFavor;
                                                                            setFieldValue(
                                                                                `detalles.${index}.aCobrar`,
                                                                                newACobrar
                                                                            );
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.recargo`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e) => {
                                                                            const newRecargo = Number(e.target.value);
                                                                            setFieldValue(
                                                                                `detalles.${index}.recargo`,
                                                                                newRecargo
                                                                            );
                                                                            const newACobrar =
                                                                                valor - bonificacion + newRecargo - aFavor;
                                                                            setFieldValue(
                                                                                `detalles.${index}.aCobrar`,
                                                                                newACobrar
                                                                            );
                                                                        }}
                                                                    />
                                                                </td>
                                                                <td className="border p-2">
                                                                    <Field
                                                                        name={`detalles.${index}.aFavor`}
                                                                        type="number"
                                                                        className="w-full"
                                                                        onChange={(e) => {
                                                                            const newAFavor = Number(e.target.value);
                                                                            setFieldValue(
                                                                                `detalles.${index}.aFavor`,
                                                                                newAFavor
                                                                            );
                                                                            const newACobrar =
                                                                                valor - bonificacion + recargo - newAFavor;
                                                                            setFieldValue(
                                                                                `detalles.${index}.aCobrar`,
                                                                                newACobrar
                                                                            );
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
                                                <Field
                                                    as="select"
                                                    name="disciplina"
                                                    className="border p-2 w-full"
                                                >
                                                    <option value="">Seleccione</option>
                                                    {disciplinasOptions.map((disc) => (
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
                                                                codigo: `DISC-${values.disciplina}`,
                                                                concepto: values.disciplina,
                                                                cuota: "",
                                                                valor: 0,
                                                                bonificacion: 0,
                                                                recargo: 0,
                                                                aFavor: 0,
                                                                importe: 0,
                                                                aCobrar: 0
                                                            });
                                                            setFieldValue("disciplina", "");
                                                        }
                                                    }}
                                                >
                                                    Grabar Disciplinas
                                                </button>
                                            </div>
                                            {/* Selector de Concepto (Producto) */}
                                            <div>
                                                <label className="block font-medium">
                                                    Concepto (Producto):
                                                </label>
                                                <Field
                                                    as="select"
                                                    name="conceptoSeleccionado"
                                                    className="border p-2 w-full"
                                                >
                                                    <option value="">Seleccione</option>
                                                    {productosOptions.map((prod) => (
                                                        <option key={prod.id} value={prod.id}>
                                                            {prod.nombre}
                                                        </option>
                                                    ))}
                                                </Field>
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
                                            <div className="col-span-2">
                                                <button
                                                    type="button"
                                                    className="bg-green-500 text-white p-2 rounded mt-4"
                                                    onClick={() => {
                                                        const selectedProduct = productosOptions.find(
                                                            (p) =>
                                                                p.id === Number(values.conceptoSeleccionado)
                                                        );
                                                        if (selectedProduct) {
                                                            const cantidad = Number(values.cantidad) || 1;
                                                            const valor = selectedProduct.valor * cantidad;
                                                            push({
                                                                codigo: selectedProduct.id,
                                                                concepto: selectedProduct.nombre,
                                                                cuota: cantidad,
                                                                valor: valor,
                                                                bonificacion: 0,
                                                                recargo: 0,
                                                                aFavor: 0,
                                                                importe: valor,
                                                                aCobrar: valor
                                                            });
                                                            setFieldValue("conceptoSeleccionado", "");
                                                            setFieldValue("cantidad", 1);
                                                        } else {
                                                            toast.error("Seleccione un producto válido");
                                                        }
                                                    }}
                                                >
                                                    Agregar
                                                </button>
                                            </div>
                                            {/* Botón para eliminar (Borrar Dis) */}
                                            <div className="col-span-2">
                                                <button
                                                    type="button"
                                                    className="bg-yellow-500 text-white p-2 rounded mt-4"
                                                    onClick={() => {
                                                        // Elimina el primer concepto que corresponda a una disciplina
                                                        const indexToRemove = values.detalles.findIndex((item) =>
                                                            String(item.codigo).startsWith("DISC-")
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
                                    <Field
                                        name="aFavor"
                                        type="number"
                                        className="border p-2 w-full"
                                        readOnly
                                    />
                                    {/* Este campo podría completarse automáticamente según el alumno */}
                                </div>
                                <div>
                                    <label className="block font-medium">Total Cobrado:</label>
                                    <Field
                                        name="totalCobrado"
                                        type="number"
                                        className="border p-2 w-full"
                                        readOnly
                                    />
                                </div>
                            </div>
                            {/* Métodos de Pago */}
                            <FieldArray name="pagos">
                                {({ push, remove }) => (
                                    <div className="mt-4">
                                        <h3 className="font-medium mb-2">Métodos de Pago</h3>
                                        {values.pagos && values.pagos.length > 0 ? (
                                            values.pagos.map((pago, index) => (
                                                <div
                                                    key={index}
                                                    className="grid grid-cols-3 gap-4 items-end mb-2"
                                                >
                                                    <div>
                                                        <label className="block font-medium">
                                                            Método de Pago:
                                                        </label>
                                                        <Field
                                                            as="select"
                                                            name={`pagos.${index}.metodo`}
                                                            className="border p-2 w-full"
                                                        >
                                                            <option value="">Seleccione</option>
                                                            {metodosPagoOptions.map((mp) => (
                                                                <option key={mp.id} value={mp.nombre}>
                                                                    {mp.nombre}
                                                                </option>
                                                            ))}
                                                        </Field>
                                                    </div>
                                                    <div>
                                                        <label className="block font-medium">Monto:</label>
                                                        <Field
                                                            name={`pagos.${index}.monto`}
                                                            type="number"
                                                            className="border p-2 w-full"
                                                        />
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
                                            onClick={() => push({ metodo: "", monto: 0 })}
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
                            <Field
                                as="textarea"
                                name="observaciones"
                                className="border p-2 w-full"
                                rows="3"
                            />
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
