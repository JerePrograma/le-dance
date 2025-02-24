// src/funcionalidades/conceptos/ConceptosFormulario.tsx
import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";
import * as Yup from "yup";
import conceptosApi from "../../api/conceptosApi";
import subConceptosApi from "../../api/subConceptosApi";
import type {
  ConceptoRegistroRequest,
  ConceptoModificacionRequest,
} from "../../types/types";
import type { ConceptoResponse } from "../../types/types";

const initialConceptoValues: ConceptoRegistroRequest & Partial<ConceptoModificacionRequest> = {
  descripcion: "",
  precio: 0,
  // Este valor se asignará luego según el subconcepto ingresado
  subConceptoId: 0,
};

const conceptoSchema = Yup.object().shape({
  descripcion: Yup.string().required("La descripción es obligatoria"),
  precio: Yup.number().positive().required("El precio es obligatorio"),
  // Ahora validamos el campo que el usuario debe llenar para el subconcepto
  subConceptoDescripcion: Yup.string().required("La descripción del subconcepto es obligatoria"),
});

interface FormValues extends ConceptoRegistroRequest, Partial<ConceptoModificacionRequest> {
  // Agregamos este campo extra que solo se usa en el formulario (no se envía al backend)
  subConceptoDescripcion: string;
}

const ConceptosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [conceptoId, setConceptoId] = useState<number | null>(null);
  const [formValues, setFormValues] = useState<FormValues>({
    ...initialConceptoValues,
    subConceptoDescripcion: "",
  });
  const [mensaje, setMensaje] = useState("");

  // Si hay un parámetro "id" en la URL, cargar el concepto para edición
  const handleBuscar = useCallback(async () => {
    const idParam = searchParams.get("id");
    if (idParam) {
      try {
        const concepto: ConceptoResponse = await conceptosApi.obtenerConceptoPorId(Number(idParam));
        setFormValues({
          descripcion: concepto.descripcion,
          precio: concepto.precio,
          subConceptoDescripcion: concepto.subConcepto.descripcion,
        });
        setConceptoId(concepto.id);
        setMensaje("Concepto encontrado.");
      } catch (error) {
        console.error("Error al buscar el concepto:", error);
        setMensaje("Concepto no encontrado.");
        setFormValues({
          ...initialConceptoValues,
          subConceptoDescripcion: "",
        });
        setConceptoId(null);
      }
    }
  }, [searchParams]);

  useEffect(() => {
    handleBuscar();
  }, [handleBuscar]);

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      try {
        // Primero, procesamos el subconcepto: buscamos por descripción (ignorar mayúsculas)
        const subDesc = values.subConceptoDescripcion.trim();
        let subConceptoId: number;
        const subExistente = await subConceptosApi.obtenerSubConceptoPorDescripcion(subDesc);
        if (subExistente) {
          subConceptoId = subExistente.id;
        } else {
          const nuevoSub = await subConceptosApi.registrarSubConcepto(subDesc);
          subConceptoId = nuevoSub.id;
        }
        // Armamos el payload para el concepto
        const payload: ConceptoRegistroRequest = {
          descripcion: values.descripcion,
          precio: values.precio,
          subConceptoId,
        };

        if (conceptoId) {
          // Para actualización, se debe incluir el campo "activo". Lo forzamos en true o según lo edites.
          await conceptosApi.actualizarConcepto(conceptoId, {
            ...payload,
            activo: true,
          } as ConceptoModificacionRequest);
          toast.success("Concepto actualizado correctamente.");
        } else {
          await conceptosApi.registrarConcepto(payload);
          toast.success("Concepto creado correctamente.");
        }
        setMensaje("Concepto guardado exitosamente.");
        navigate("/conceptos");
      } catch (error) {
        console.error("Error al guardar el concepto:", error);
        toast.error("Error al guardar el concepto.");
        setMensaje("Error al guardar el concepto.");
      }
    },
    [conceptoId, navigate]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">Formulario de Concepto</h1>
      {mensaje && (
        <p className={`form-mensaje ${mensaje.includes("Error") ? "form-mensaje-error" : "form-mensaje-success"}`}>
          {mensaje}
        </p>
      )}
      <Formik
        initialValues={formValues}
        validationSchema={conceptoSchema}
        onSubmit={handleSubmit}
        enableReinitialize
      >
        {({ isSubmitting, resetForm }) => (
          <Form className="formulario max-w-4xl mx-auto">
            {/* Se muestran los campos editables: Descripción, Precio y Subconcepto */}
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="mb-4">
                <label htmlFor="descripcion" className="auth-label">
                  Descripción:
                </label>
                <Field name="descripcion" type="text" className="form-input" />
                <ErrorMessage name="descripcion" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="precio" className="auth-label">
                  Precio:
                </label>
                <Field name="precio" type="number" className="form-input" />
                <ErrorMessage name="precio" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="subConceptoDescripcion" className="auth-label">
                  Subconcepto:
                </label>
                <Field name="subConceptoDescripcion" type="text" className="form-input" placeholder="Ingrese el subconcepto" />
                <ErrorMessage name="subConceptoDescripcion" component="div" className="auth-error" />
              </div>
            </div>

            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting} className="page-button">
                {conceptoId ? "Actualizar" : "Crear"} Concepto
              </Boton>
              <Boton type="button" onClick={() => resetForm()} className="page-button-secondary">
                Limpiar
              </Boton>
              <Boton type="button" onClick={() => navigate("/conceptos")} className="page-button-secondary">
                Cancelar
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default ConceptosFormulario;
