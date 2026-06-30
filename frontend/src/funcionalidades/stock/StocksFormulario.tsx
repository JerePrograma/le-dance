import { useEffect, useState } from "react";
import { ErrorMessage, Field, Form, Formik, type FormikHelpers } from "formik";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import stocksApi from "../../api/stocksApi";
import Boton from "../../componentes/comunes/Boton";
import type { StockModificacionRequest, StockRegistroRequest } from "../../types/types";
import { stockEsquema } from "../../validaciones/stockEsquema";

type StockForm = Omit<StockModificacionRequest, "idempotencyKey">;

const VACIO: StockForm = {
  nombre: "",
  precio: "",
  stock: 0,
  requiereControlDeStock: false,
  codigoBarras: "",
  activo: true,
};

export default function StocksFormulario() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const id = Number(searchParams.get("id")) || undefined;
  const [inicial, setInicial] = useState<StockForm>(VACIO);

  useEffect(() => {
    if (!id) return;
    stocksApi.obtenerStockPorId(id)
      .then(({ nombre, precio, stock, requiereControlDeStock, codigoBarras, activo }) =>
        setInicial({ nombre, precio, stock, requiereControlDeStock, codigoBarras, activo }))
      .catch(() => toast.error("No se pudo cargar el producto"));
  }, [id]);

  const guardar = async (values: StockForm, helpers: FormikHelpers<StockForm>) => {
    const request: StockRegistroRequest = { ...values, idempotencyKey: crypto.randomUUID() };
    try {
      if (id) await stocksApi.actualizarStock(id, { ...request, activo: values.activo });
      else await stocksApi.registrarStock(request);
      toast.success(id ? "Producto actualizado" : "Producto creado");
      navigate("/stocks");
    } catch {
      toast.error("No se pudo guardar el producto");
    } finally {
      helpers.setSubmitting(false);
    }
  };

  return <main className="form-container">
    <h1>{id ? "Editar producto" : "Registrar producto"}</h1>
    <Formik initialValues={inicial} validationSchema={stockEsquema} onSubmit={guardar} enableReinitialize>
      {({ isSubmitting }) => <Form>
        <label>Nombre<Field name="nombre" /><ErrorMessage name="nombre" component="span" /></label>
        <label>Precio<Field name="precio" inputMode="decimal" /><ErrorMessage name="precio" component="span" /></label>
        <label>Cantidad<Field name="stock" type="number" min="0" /><ErrorMessage name="stock" component="span" /></label>
        <label>Código de barras<Field name="codigoBarras" /></label>
        <label><Field name="requiereControlDeStock" type="checkbox" /> Requiere control de stock</label>
        {id && <label><Field name="activo" type="checkbox" /> Activo</label>}
        <Boton type="submit" disabled={isSubmitting}>Guardar</Boton>
        <Boton type="button" onClick={() => navigate("/stocks")}>Cancelar</Boton>
      </Form>}
    </Formik>
  </main>;
}
