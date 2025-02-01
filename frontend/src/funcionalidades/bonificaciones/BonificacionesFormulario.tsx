// src/funcionalidades/bonificaciones/BonificacionesFormulario.tsx
import React, { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Boton from "../../componentes/comunes/Boton";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { bonificacionEsquema } from "../../validaciones/bonificacionEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";

interface Bonificacion {
  id?: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

const initialBonificacionValues: Bonificacion = {
  descripcion: "",
  porcentajeDescuento: 0,
  activo: true,
  observaciones: "",
};

const BonificacionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleBuscar = useCallback(
    async (idStr: string, resetForm: (values: Bonificacion) => void) => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID inválido");
          return;
        }
        const response = await api.get<Bonificacion>(
          `/api/bonificaciones/${idNum}`
        );
        resetForm(response.data);
        toast.success("Bonificación cargada correctamente.");
      } catch {
        toast.error("Error al cargar la bonificación.");
        resetForm(initialBonificacionValues);
      }
    },
    []
  );

  useEffect(() => {
    const idParam = searchParams.get("id");
    if (idParam) handleBuscar(idParam, () => {});
  }, [searchParams, handleBuscar]);

  const handleGuardar = useCallback(async (values: Bonificacion) => {
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
  }, []);

  // Hasta aquí se finaliza la parte de inicialización y lógica para BonificacionesFormulario

  return (
    <div className="formulario">
      <h1 className="form-title">Formulario de Bonificación</h1>
      <Formik
        initialValues={initialBonificacionValues}
        validationSchema={bonificacionEsquema}
        onSubmit={handleGuardar}
      >
        {({ resetForm, isSubmitting }) => (
          <Form className="formulario">
            <div className="form-grid">
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
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => resetForm({ values: initialBonificacionValues })}
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                secondary
                onClick={() => navigate("/bonificaciones")}
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

export default BonificacionesFormulario;
