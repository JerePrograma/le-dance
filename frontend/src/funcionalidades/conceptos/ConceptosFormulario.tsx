import { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";
import * as Yup from "yup";
import conceptosApi from "../../api/conceptosApi";
import subConceptosApi from "../../api/subConceptosApi";
import useDebounce from "../../hooks/useDebounce";
import type {
  ConceptoRegistroRequest,
  ConceptoModificacionRequest,
} from "../../types/types";
import type { ConceptoResponse, SubConceptoResponse } from "../../types/types";

const initialConceptoValues: ConceptoRegistroRequest & Partial<ConceptoModificacionRequest> = {
  descripcion: "",
  precio: 0,
  // Este valor se asignara luego segun el subconcepto seleccionado
  subConceptoId: 0,
};

const conceptoSchema = Yup.object().shape({
  descripcion: Yup.string().required("La descripcion es obligatoria"),
  precio: Yup.number().positive().required("El precio es obligatorio"),
  subConceptoDescripcion: Yup.string().required("La descripcion del subconcepto es obligatoria"),
});

type FormValues = ConceptoRegistroRequest & Partial<ConceptoModificacionRequest> & {
  subConceptoDescripcion: string;
  id?: number; // Campo opcional para edicion
};

const ConceptosFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [conceptoId, setConceptoId] = useState<number | null>(null);
  const [formValues, setFormValues] = useState<FormValues>({
    ...initialConceptoValues,
    subConceptoDescripcion: "",
  });
  const [mensaje, setMensaje] = useState("");

  // Estados para la busqueda de subconcepto
  const [subConceptoBusqueda, setSubConceptoBusqueda] = useState("");
  const debouncedSubConceptoBusqueda = useDebounce(subConceptoBusqueda, 300);
  const [sugerenciasSubConceptos, setSugerenciasSubConceptos] = useState<SubConceptoResponse[]>([]);

  // Si hay un parametro "id" en la URL, cargar el concepto para edicion
  const handleBuscar = useCallback(async () => {
    const idParam = searchParams.get("id");
    if (idParam) {
      try {
        const concepto: ConceptoResponse = await conceptosApi.obtenerConceptoPorId(Number(idParam));
        setFormValues({
          id: concepto.id,
          descripcion: concepto.descripcion,
          precio: concepto.precio,
          subConceptoDescripcion: concepto.subConcepto.descripcion,
          subConceptoId: concepto.subConcepto.id,
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

  // Buscar sugerencias de subconceptos a partir del texto ingresado
  useEffect(() => {
    const buscarSugerencias = async () => {
      if (debouncedSubConceptoBusqueda) {
        try {
          const sugerencias = await subConceptosApi.buscarSubConceptos(debouncedSubConceptoBusqueda);
          setSugerenciasSubConceptos(sugerencias);
        } catch (error) {
          console.error("Error al buscar subconceptos:", error);
          setSugerenciasSubConceptos([]);
        }
      } else {
        setSugerenciasSubConceptos([]);
      }
    };
    buscarSugerencias();
  }, [debouncedSubConceptoBusqueda]);

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      try {
        // Si ya se selecciono un subconcepto (campo subConceptoId distinto de 0), lo usamos;
        // de lo contrario, se realiza la busqueda por descripcion.
        let subConceptoIdFinal: number;
        if (values.subConceptoId && values.subConceptoId !== 0) {
          subConceptoIdFinal = values.subConceptoId;
        } else {
          const subDesc = values.subConceptoDescripcion.trim().toUpperCase();
          const subExistente = await subConceptosApi.obtenerSubConceptoPorDescripcion(subDesc);
          if (subExistente) {
            subConceptoIdFinal = subExistente.id;
          } else {
            const nuevoSub = await subConceptosApi.registrarSubConcepto({ descripcion: subDesc });
            subConceptoIdFinal = nuevoSub.id;
          }
        }

        const payload: ConceptoRegistroRequest = {
          descripcion: values.descripcion,
          precio: values.precio,
          subConceptoId: subConceptoIdFinal,
        };

        if (conceptoId) {
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
        {({ isSubmitting, resetForm, setFieldValue }) => (
          <Form className="formulario max-w-4xl mx-auto">
            {/* Campos principales: Descripcion y Precio */}
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="mb-4">
                <label htmlFor="descripcion" className="auth-label">
                  Descripcion:
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
            </div>

            {/* Campo de Subconcepto con autocompletado */}
            <div className="mb-4">
              <label htmlFor="subConceptoDescripcion" className="auth-label">
                Subconcepto:
              </label>
              <div className="relative">
                <Field
                  name="subConceptoDescripcion"
                  type="text"
                  className="form-input"
                  placeholder="Ingrese el subconcepto"
                  style={{ textTransform: "uppercase" }}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    const value = e.target.value.toUpperCase();
                    setFieldValue("subConceptoDescripcion", value);
                    // Si el usuario escribe manualmente, reiniciamos el subConceptoId
                    setFieldValue("subConceptoId", 0);
                    setSubConceptoBusqueda(value);
                  }}
                />
                <ErrorMessage name="subConceptoDescripcion" component="div" className="auth-error" />
                {sugerenciasSubConceptos.length > 0 && (
                  <ul className="absolute z-10 w-full bg-white border border-gray-200 max-h-48 overflow-auto mt-1">
                    {sugerenciasSubConceptos.map((sub) => (
                      <li
                        key={sub.id}
                        className="p-2 hover:bg-gray-100 cursor-pointer"
                        onClick={() => {
                          // Al seleccionar, actualizamos ambos campos
                          setFieldValue("subConceptoDescripcion", sub.descripcion);
                          setFieldValue("subConceptoId", sub.id);
                          setSubConceptoBusqueda(sub.descripcion);
                          setSugerenciasSubConceptos([]);
                        }}
                      >
                        {sub.descripcion}
                      </li>
                    ))}
                  </ul>
                )}
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
