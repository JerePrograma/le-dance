import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage, type FormikHelpers } from "formik";
import { stockEsquema } from "../../validaciones/stockEsquema";
import stocksApi from "../../api/stocksApi";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import type {
    StockRegistroRequest,
    StockModificacionRequest,
    StockResponse,
} from "../../types/types";

const initialStockValues: StockRegistroRequest & Partial<StockModificacionRequest> = {
    nombre: "",
    precio: 0,
    tipoStockId: 0,
    stock: 0,
    requiereControlDeStock: false,
    codigoBarras: "",
    // Para el registro, no es necesario enviar "activo" (lo agregamos solo en edicion)
    activo: true,
    fechaIngreso: new Date().toISOString().split("T")[0],
    fechaEgreso: undefined,
};

const StocksFormulario: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [stockId, setStockId] = useState<number | null>(null);
    const [formValues, setFormValues] = useState(initialStockValues);
    const [mensaje, setMensaje] = useState("");
    const [idBusqueda, setIdBusqueda] = useState("");

    const convertToStockFormValues = useCallback(
        (stock: StockResponse): StockRegistroRequest & Partial<StockModificacionRequest> => {
            return {
                nombre: stock.nombre,
                precio: stock.precio,
                tipoStockId: stock.tipo.id,
                stock: stock.stock,
                requiereControlDeStock: stock.requiereControlDeStock,
                codigoBarras: stock.codigoBarras,
                activo: stock.activo,
                // Nuevos atributos
                fechaIngreso: stock.fechaIngreso,
                fechaEgreso: stock.fechaEgreso,
            };
        },
        []
    );

    const handleBuscar = useCallback(async () => {
        if (!idBusqueda) {
            setMensaje("Por favor, ingrese un ID de stock.");
            return;
        }
        try {
            const stock = await stocksApi.obtenerStockPorId(Number(idBusqueda));
            const converted = convertToStockFormValues(stock);
            setFormValues(converted);
            setStockId(stock.id);
            setMensaje("Stock encontrado.");
        } catch (error) {
            toast.error("Error al buscar el stock:");
            setMensaje("Stock no encontrado.");
            resetearFormulario();
        }
    }, [idBusqueda, convertToStockFormValues]);

    const resetearFormulario = useCallback(() => {
        setFormValues(initialStockValues);
        setStockId(null);
        setMensaje("");
        setIdBusqueda("");
    }, []);

    useEffect(() => {
        const idParam = searchParams.get("id");
        if (idParam) {
            setIdBusqueda(idParam);
            handleBuscar();
        }
    }, [searchParams, handleBuscar]);


    const convertirAStockModificacionRequest = (
        values: StockRegistroRequest & Partial<StockModificacionRequest>
    ): StockModificacionRequest => ({
        nombre: values.nombre,
        precio: values.precio,
        // Convertimos el campo para que coincida con lo que espera el backend:
        tipoStockId: values.tipoStockId,
        stock: values.stock,
        requiereControlDeStock: values.requiereControlDeStock,
        codigoBarras: values.codigoBarras,
        // Para la modificacion el backend requiere "activo"
        activo: values.activo as boolean,
        fechaIngreso: values.fechaIngreso,
        fechaEgreso: values.fechaEgreso,
    });

    const handleGuardar = useCallback(
        async (
            values: StockRegistroRequest & Partial<StockModificacionRequest>,
            { setSubmitting }: FormikHelpers<StockRegistroRequest & Partial<StockModificacionRequest>>
        ) => {
            try {
                // Convierte el valor vacio de tipoEgreso a null
                const valoresAEnviar = {
                    ...values,
                };

                if (stockId) {
                    // Para actualizacion, si es necesario, realiza la transformacion tambien en la funcion de conversion
                    const stockModificacion = convertirAStockModificacionRequest(valoresAEnviar);
                    await stocksApi.actualizarStock(stockId, stockModificacion);
                    toast.success("Stock actualizado correctamente.");
                } else {
                    await stocksApi.registrarStock(valoresAEnviar as StockRegistroRequest);
                    toast.success("Stock creado correctamente.");
                }
                setMensaje("Stock guardado exitosamente.");
            } catch (error) {
                toast.error("Error al guardar el stock.");
                setMensaje("Error al guardar el stock.");
            } finally {
                setSubmitting(false);
            }
        },
        [stockId]
    );

    return (
        <div className="page-container">
            <h1 className="page-title">Formulario de Stock</h1>
            <Formik
                initialValues={formValues}
                validationSchema={stockEsquema}
                onSubmit={handleGuardar}
                enableReinitialize
            >
                {({ isSubmitting, resetForm }) => (
                    <Form className="formulario max-w-4xl mx-auto">
                        <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div className="col-span-full mb-4">
                                <label htmlFor="idBusqueda" className="auth-label">
                                    ID de Stock:
                                </label>
                                <div className="flex gap-2">
                                    <input
                                        type="number"
                                        id="idBusqueda"
                                        value={idBusqueda}
                                        onChange={(e) => setIdBusqueda(e.target.value)}
                                        className="form-input flex-grow"
                                    />
                                    <Boton onClick={handleBuscar} type="button" className="page-button">
                                        Buscar
                                    </Boton>
                                </div>
                            </div>
                            <div className="mb-4">
                                <label htmlFor="nombre" className="auth-label">
                                    Nombre:
                                </label>
                                <Field name="nombre" type="text" className="form-input" />
                                <ErrorMessage name="nombre" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="precio" className="auth-label">
                                    Precio:
                                </label>
                                <Field name="precio" type="number" className="form-input" />
                                <ErrorMessage name="precio" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="stock" className="auth-label">
                                    Stock:
                                </label>
                                <Field name="stock" type="number" className="form-input" />
                                <ErrorMessage name="stock" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="requiereControlDeStock" className="auth-label">
                                    Requiere Control de Stock:
                                </label>
                                <Field name="requiereControlDeStock">
                                    {({ field, form }: any) => (
                                        <select
                                            {...field}
                                            className="form-input"
                                            onChange={(e) =>
                                                form.setFieldValue("requiereControlDeStock", e.target.value === "true")
                                            }
                                        >
                                            <option value="false">No</option>
                                            <option value="true">Si</option>
                                        </select>
                                    )}
                                </Field>
                                <ErrorMessage name="requiereControlDeStock" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="fechaIngreso" className="auth-label">
                                    Fecha de Ingreso:
                                </label>
                                <Field name="fechaIngreso" type="date" className="form-input" />
                                <ErrorMessage name="fechaIngreso" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="fechaEgreso" className="auth-label">
                                    Fecha de Egreso (opcional):
                                </label>
                                <Field name="fechaEgreso" type="date" className="form-input" />
                                <ErrorMessage name="fechaEgreso" component="div" className="auth-error" />
                            </div>
                            {stockId !== null && (
                                <div className="col-span-full mb-4">
                                    <label className="flex items-center space-x-2">
                                        <Field type="checkbox" name="activo" className="form-checkbox" />
                                        <span>Activo</span>
                                    </label>
                                    <ErrorMessage name="activo" component="div" className="auth-error" />
                                </div>
                            )}
                        </div>

                        {mensaje && (
                            <p
                                className={`form-mensaje ${mensaje.includes("Error") || mensaje.includes("no encontrado")
                                    ? "form-mensaje-error"
                                    : "form-mensaje-success"
                                    }`}
                            >
                                {mensaje}
                            </p>
                        )}

                        <div className="form-acciones">
                            <Boton type="submit" disabled={isSubmitting} className="page-button">
                                Guardar
                            </Boton>
                            <Boton
                                type="button"
                                onClick={() => {
                                    resetearFormulario();
                                    resetForm();
                                }}
                                className="page-button-secondary"
                            >
                                Limpiar
                            </Boton>
                            <Boton
                                type="button"
                                onClick={() => navigate("/stocks")}
                                className="page-button-secondary"
                            >
                                Volver al Listado
                            </Boton>
                        </div>
                    </Form>
                )}
            </Formik>
        </div>
    );
};

export default StocksFormulario;
