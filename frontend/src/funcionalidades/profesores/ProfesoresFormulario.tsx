import type React from "react";
import { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Boton from "../../componentes/comunes/Boton";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { profesorEsquema } from "../../validaciones/profesorEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import { Search } from "lucide-react";

interface ProfesorRequest {
  id?: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const initialProfesorValues: ProfesorRequest = {
  nombre: "",
  apellido: "",
  especialidad: "",
  aniosExperiencia: 0,
};

const ProfesoresFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const handleBuscar = useCallback(
    async (idStr: string, resetForm: (values: ProfesorRequest) => void) => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID inválido");
          return;
        }
        const data: ProfesorRequest = await api.get(`/api/profesores/${idNum}`);
        resetForm(data);
        toast.success("Profesor cargado correctamente.");
      } catch {
        toast.error("Error al cargar los datos del profesor.");
        resetForm(initialProfesorValues);
      }
    },
    []
  );

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id, () => {});
    }
  }, [searchParams, handleBuscar]);

  const handleGuardar = useCallback(async (values: ProfesorRequest) => {
    try {
      if (values.id) {
        await api.put(`/api/profesores/${values.id}`, values);
        toast.success("Profesor actualizado correctamente.");
      } else {
        await api.post("/api/profesores", values);
        toast.success("Profesor creado correctamente.");
      }
    } catch {
      toast.error("Error al guardar los datos del profesor.");
    }
  }, []);

  return (
    <div className="page-container">
      <h1 className="page-title">
        {searchParams.get("id") ? "Editar Profesor" : "Nuevo Profesor"}
      </h1>
      <Formik
        initialValues={initialProfesorValues}
        validationSchema={profesorEsquema}
        onSubmit={handleGuardar}
        enableReinitialize
      >
        {({ resetForm, isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Profesor:
                </label>
                <div className="flex gap-2">
                  <Field
                    type="number"
                    id="idBusqueda"
                    name="id"
                    className="form-input flex-grow"
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleBuscar(e.target.value, (vals) =>
                        resetForm({ values: vals })
                      )
                    }
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

              {[
                { name: "nombre", label: "Nombre (obligatorio)" },
                { name: "apellido", label: "Apellido (obligatorio)" },
                { name: "especialidad", label: "Especialidad (obligatorio)" },
                {
                  name: "activo",
                  label: "activo",
                  type: "boolean",
                },
              ].map(({ name, label, type = "text" }) => (
                <div key={name} className="mb-4">
                  <label htmlFor={name} className="auth-label">
                    {label}:
                  </label>
                  <Field
                    name={name}
                    type={type}
                    id={name}
                    className="form-input"
                  />
                  <ErrorMessage
                    name={name}
                    component="div"
                    className="auth-error"
                  />
                </div>
              ))}
            </div>

            <div className="form-acciones">
              <Boton
                type="submit"
                disabled={isSubmitting}
                className="page-button"
              >
                Guardar Profesor
              </Boton>
              <Boton
                type="reset"
                onClick={() => resetForm({ values: initialProfesorValues })}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                onClick={() => navigate("/profesores")}
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

export default ProfesoresFormulario;
