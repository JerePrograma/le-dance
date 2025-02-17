// src/funcionalidades/stocks/StocksFormulario.tsx
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { stockEsquema } from "../../validaciones/stockEsquema"; // Define tu esquema de validación
import stocksApi from "../../api/stocksApi";
import tipoStocksApi from "../../api/tipoStocksApi";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import type { StockRegistroRequest, StockModificacionRequest, StockResponse } from "../../types/types";
import type { TipoStockResponse } from "../../types/types";

const initialStockValues: StockRegistroRequest & Partial<StockModificacionRequest> = {
    nombre: "",
    precio: 0,
    tipoStockId: undefined,
    stock: 0,
    requiereControlDeStock: false,
    codigoBarras: "",
    activo: true,
};

const StocksFormulario: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [stockId, setStockId] = useState<number | null>(null);
    const [formValues, setFormValues] = useState(initialStockValues);
    const [tipoStocks, setTipoStocks] = useState<TipoStockResponse[]>([]);
    const [mensaje, setMensaje] = useState("");
    const [idBusqueda, setIdBusqueda] = useState("");

    const fetchTipoStocks = useCallback(async () => {
        try {
            const response = await tipoStocksApi.listarTiposStockActivos();
            setTipoStocks(response);
        } catch (error) {
            console.error("Error al cargar tipos de stock:", error);
        }
    }, []);

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
            console.error("Error al buscar el stock:", error);
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
        fetchTipoStocks();
        const idParam = searchParams.get("id");
        if (idParam) {
            setIdBusqueda(idParam);
            handleBuscar();
        }
    }, [searchParams, handleBuscar, fetchTipoStocks]);

    const handleGuardar = useCallback(
        async (values: StockRegistroRequest & Partial<StockModificacionRequest>) => {
            try {
                if (stockId) {
                    await stocksApi.actualizarStock(stockId, values as StockModificacionRequest);
                    toast.success("Stock actualizado correctamente.");
                } else {
                    const nuevoStock = await stocksApi.registrarStock(values as StockRegistroRequest);
                    setStockId(nuevoStock.id);
                    toast.success("Stock creado correctamente.");
                }
                setMensaje("Stock guardado exitosamente.");
            } catch (error) {
                console.error("Error al guardar el stock:", error);
                toast.error("Error al guardar el stock.");
                setMensaje("Error al guardar el stock.");
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
                                <label htmlFor="tipoStockId" className="auth-label">
                                    Tipo de Stock:
                                </label>
                                <Field as="select" name="tipoStockId" className="form-input">
                                    <option value="">Seleccione un tipo de stock</option>
                                    {tipoStocks.map((tipo) => (
                                        <option key={tipo.id} value={tipo.id}>
                                            {tipo.descripcion}
                                        </option>
                                    ))}
                                </Field>
                                <ErrorMessage name="tipoStockId" component="div" className="auth-error" />
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
                                <Field as="select" name="requiereControlDeStock" className="form-input">
                                    <option value={false}>No</option>
                                    <option value={true}>Sí</option>
                                </Field>
                                <ErrorMessage name="requiereControlDeStock" component="div" className="auth-error" />
                            </div>
                            <div className="mb-4">
                                <label htmlFor="codigoBarras" className="auth-label">
                                    Código de Barras:
                                </label>
                                <Field name="codigoBarras" type="text" className="form-input" />
                                <ErrorMessage name="codigoBarras" component="div" className="auth-error" />
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
                            <Boton type="button" onClick={() => navigate("/stocks")} className="page-button-secondary">
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
