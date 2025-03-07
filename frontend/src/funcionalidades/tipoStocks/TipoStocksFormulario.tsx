import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { tipoStockEsquema } from "../../validaciones/tipoStockEsquema";
import tipoStocksApi from "../../api/tipoStocksApi";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import type { TipoStockRegistroRequest, TipoStockModificacionRequest, TipoStockResponse } from "../../types/types";

const initialTipoStockValues: TipoStockRegistroRequest & Partial<TipoStockModificacionRequest> = {
    descripcion: "",
    activo: true,
};

const TipoStocksFormulario: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [tipoId, setTipoId] = useState<number | null>(null);
    const [formValues, setFormValues] = useState(initialTipoStockValues);
    const [mensaje, setMensaje] = useState("");
    const [idBusqueda, setIdBusqueda] = useState("");

    const convertToTipoStockFormValues = useCallback(
        (tipo: TipoStockResponse): TipoStockRegistroRequest & Partial<TipoStockModificacionRequest> => ({
            descripcion: tipo.descripcion,
            activo: tipo.activo,
        }),
        []
    );

    const handleBuscar = useCallback(async () => {
        if (!idBusqueda) {
            setMensaje("Por favor, ingrese un ID de tipo de stock.");
            return;
        }
        try {
            const tipo = await tipoStocksApi.obtenerTipoStockPorId(Number(idBusqueda));
            const converted = convertToTipoStockFormValues(tipo);
            setFormValues(converted);
            setTipoId(tipo.id);
            setMensaje("Tipo de stock encontrado.");
        } catch (error) {
            toast.error("Error al buscar el tipo de stock:");
            setMensaje("Tipo de stock no encontrado.");
            resetearFormulario();
        }
    }, [idBusqueda, convertToTipoStockFormValues]);

    const resetearFormulario = useCallback(() => {
        setFormValues(initialTipoStockValues);
        setTipoId(null);
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

    const handleGuardar = useCallback(
        async (values: TipoStockRegistroRequest & Partial<TipoStockModificacionRequest>) => {
            try {
                if (tipoId) {
                    await tipoStocksApi.actualizarTipoStock(tipoId, values as TipoStockModificacionRequest);
                    toast.success("Tipo de stock actualizado correctamente.");
                } else {
                    const nuevoTipo = await tipoStocksApi.registrarTipoStock(values as TipoStockRegistroRequest);
                    setTipoId(nuevoTipo.id);
                    toast.success("Tipo de stock creado correctamente.");
                }
                setMensaje("Tipo de stock guardado exitosamente.");
            } catch (error) {
                toast.error("Error al guardar el tipo de stock.");
                setMensaje("Error al guardar el tipo de stock.");
            }
        },
        [tipoId]
    );

    return (
        <div className="page-container">
            <h1 className="page-title">Formulario de Tipo de Stock</h1>
            <Formik
                initialValues={formValues}
                validationSchema={tipoStockEsquema}
                onSubmit={handleGuardar}
                enableReinitialize
            >
                {({ isSubmitting, resetForm }) => (
                    <Form className="formulario max-w-4xl mx-auto">
                        <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div className="col-span-full mb-4">
                                <label htmlFor="idBusqueda" className="auth-label">
                                    ID de Tipo de Stock:
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
                                <label htmlFor="descripcion" className="auth-label">
                                    Descripcion:
                                </label>
                                <Field name="descripcion" type="text" className="form-input" />
                                <ErrorMessage name="descripcion" component="div" className="auth-error" />
                            </div>
                            {tipoId !== null && (
                                <div className="col-span-full mb-4">
                                    <label className="flex items-center space-x-2">
                                        <Field type="checkbox" name="activo" className="form-checkbox" />
                                        <span>Activo</span>
                                    </label>
                                    <ErrorMessage name="activo" component="div" className="auth-error" />
                                </div>
                            )}
                        </div>
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
                            <Boton type="button" onClick={() => navigate("/tipo-stocks")} className="page-button-secondary">
                                Volver al Listado
                            </Boton>
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
                    </Form>
                )}
            </Formik>
        </div>
    );
};

export default TipoStocksFormulario;
