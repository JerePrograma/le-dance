import React, { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { bonificacionEsquema } from "../../validaciones/bonificacionEsquema";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

interface Bonificacion {
  id?: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const BonificacionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const initialValues: Bonificacion = {
    descripcion: "",
    porcentajeDescuento: 0,
    activo: true,
    observaciones: "",
  };

  const handleBuscar = useCallback(async (idStr: string, setValues: any) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID inválido");
        return;
      }
      const response = await api.get<Bonificacion>(
        `/api/bonificaciones/${idNum}`
      );
      setValues(response.data);
      toast.success("Bonificación cargada correctamente.");
    } catch {
      toast.error("Error al cargar la bonificación.");
    }
  }, []);

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) handleBuscar(idParam, () => {});
  }, [searchParams, handleBuscar]);

  const handleGuardar = async (values: Bonificacion) => {
    try {
      if (values.id) {
        await api.put(`/api/bonificaciones/${values.id}`, values);
        toast.success("Bonificación actualizada correctamente.");
      } else {
        await api.post("/api/bonificaciones", values);
        toast.success("Bonificación creada correctamente.");
      }
    } catch {
      toast.error("Error al guardar la bonificación.");
    }
  };

  const handleVolverListado = () => {
    navigate("/bonificaciones");
  };

  return (
    <div className="formulario">
      <h1 className="form-titulo">Formulario de Bonificación</h1>

      <Formik
        initialValues={initialValues}
        validationSchema={bonificacionEsquema}
        onSubmit={handleGuardar}
      >
        {({ setValues, isSubmitting }) => (
          <Form className="formulario">
            <div className="form-grid">
              {/* Descripción */}
              <div>
                <label>Descripción (obligatoria):</label>
                <Field
                  type="text"
                  name="descripcion"
                  placeholder="Ejemplo: 1/2 BECA"
                  className="form-input"
                />
                <ErrorMessage
                  name="descripcion"
                  component="div"
                  className="error"
                />
              </div>

              {/* Porcentaje de Descuento */}
              <div>
                <label>Porcentaje de Descuento:</label>
                <Field
                  type="number"
                  name="porcentajeDescuento"
                  placeholder="Ejemplo: 50"
                  className="form-input"
                />
                <ErrorMessage
                  name="porcentajeDescuento"
                  component="div"
                  className="error"
                />
              </div>

              {/* Observaciones */}
              <div>
                <label>Observaciones:</label>
                <Field
                  as="textarea"
                  name="observaciones"
                  className="form-input"
                />
                <ErrorMessage
                  name="observaciones"
                  component="div"
                  className="error"
                />
              </div>

              {/* Activo */}
              <div>
                <label>Activo:</label>
                <Field
                  type="checkbox"
                  name="activo"
                  className="form-checkbox"
                />
                <ErrorMessage name="activo" component="div" className="error" />
              </div>
            </div>

            {/* Botones de acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => setValues(initialValues)}
              >
                Limpiar
              </Boton>
              <Boton type="button" secondary onClick={handleVolverListado}>
                Volver al Listado
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default BonificacionesFormulario;
