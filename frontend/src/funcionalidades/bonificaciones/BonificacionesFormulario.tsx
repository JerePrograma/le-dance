import type React from "react";
import { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Boton from "../../componentes/comunes/Boton";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { bonificacionEsquema } from "../../validaciones/bonificacionEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import { Search } from "lucide-react";

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

  return (
    <div className="page-container">
      <h1 className="page-title">Formulario de Bonificación</h1>
      <Formik
        initialValues={initialBonificacionValues}
        validationSchema={bonificacionEsquema}
        onSubmit={handleGuardar}
        enableReinitialize
      >
        {({ resetForm, isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Bonificación:
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    id="idBusqueda"
                    className="form-input flex-grow"
                    onChange={(e) => {
                      const id = e.target.value;
                      handleBuscar(id, (vals) => resetForm({ values: vals }));
                    }}
                  />
                  <Boton
                    onClick={() =>
                      handleBuscar(values.id?.toString() || "", (vals) =>
                        resetForm({ values: vals })
                      )
                    }
                    className="page-button"
                  >
                    <Search className="w-5 h-5 mr-2" />
                    Buscar
                  </Boton>
                </div>
              </div>

              <div className="mb-4">
                <label htmlFor="descripcion" className="auth-label">
                  Descripción (obligatoria):
                </label>
                <Field
                  type="text"
                  id="descripcion"
                  name="descripcion"
                  placeholder="Ejemplo: 1/2 BECA"
                  className="form-input"
                />
                <ErrorMessage
                  name="descripcion"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="porcentajeDescuento" className="auth-label">
                  Porcentaje de Descuento:
                </label>
                <Field
                  type="number"
                  id="porcentajeDescuento"
                  name="porcentajeDescuento"
                  placeholder="Ejemplo: 50"
                  className="form-input"
                />
                <ErrorMessage
                  name="porcentajeDescuento"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="col-span-full mb-4">
                <label htmlFor="observaciones" className="auth-label">
                  Observaciones:
                </label>
                <Field
                  as="textarea"
                  id="observaciones"
                  name="observaciones"
                  className="form-input h-24"
                />
                <ErrorMessage
                  name="observaciones"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="col-span-full mb-4">
                <label className="flex items-center space-x-2">
                  <Field
                    type="checkbox"
                    name="activo"
                    className="form-checkbox"
                  />
                  <span>Activo</span>
                </label>
                <ErrorMessage
                  name="activo"
                  component="div"
                  className="auth-error"
                />
              </div>
            </div>

            <div className="form-acciones">
              <Boton
                type="submit"
                disabled={isSubmitting}
                className="page-button"
              >
                Guardar
              </Boton>
              <Boton
                type="reset"
                onClick={() => resetForm({ values: initialBonificacionValues })}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                onClick={() => navigate("/bonificaciones")}
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

export default BonificacionesFormulario;
