import React, { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { profesorEsquema } from "../../validaciones/profesorEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";

interface ProfesorRequest {
  id?: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const ProfesoresFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const initialValues: ProfesorRequest = {
    nombre: "",
    apellido: "",
    especialidad: "",
    aniosExperiencia: 0,
  };

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) handleBuscar(id, () => {});
  }, [searchParams]);

  const handleBuscar = useCallback(async (idStr: string, setValues: any) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID inválido");
        return;
      }
      const data: ProfesorRequest = await api.get(`/api/profesores/${idNum}`);
      setValues({
        nombre: data.nombre,
        apellido: data.apellido,
        especialidad: data.especialidad,
        aniosExperiencia: data.aniosExperiencia ?? 0,
      });
      toast.success("Profesor cargado correctamente.");
    } catch {
      toast.error("Error al cargar los datos del profesor.");
    }
  }, []);

  const handleGuardar = async (values: ProfesorRequest) => {
    try {
      if (!values.nombre || !values.apellido || !values.especialidad) {
        toast.error("Por favor, complete todos los campos obligatorios.");
        return;
      }

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
  };

  const handleLimpiar = (setValues: any) => {
    setValues(initialValues);
  };

  const handleVolverListado = () => {
    navigate("/profesores");
  };

  return (
    <div className="formulario">
      <h1 className="form-title">
        {searchParams.get("id") ? "Editar Profesor" : "Nuevo Profesor"}
      </h1>

      <Formik
        initialValues={initialValues}
        validationSchema={profesorEsquema}
        onSubmit={handleGuardar}
      >
        {({ setValues, isSubmitting }) => (
          <Form className="formulario">
            {/* Búsqueda por ID */}
            <div className="form-busqueda">
              <label htmlFor="idBusqueda">Número de Profesor:</label>
              <Field
                type="number"
                id="idBusqueda"
                name="id"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  handleBuscar(e.target.value, setValues)
                }
              />
              <Boton
                type="button"
                onClick={() =>
                  handleBuscar(searchParams.get("id") || "", setValues)
                }
              >
                Buscar
              </Boton>
            </div>

            {/* Datos del Profesor */}
            <fieldset className="form-fieldset">
              <legend>Datos del Profesor</legend>
              <div className="form-grid">
                {[
                  { name: "nombre", label: "Nombre (obligatorio)" },
                  { name: "apellido", label: "Apellido (obligatorio)" },
                  { name: "especialidad", label: "Especialidad (obligatorio)" },
                  {
                    name: "aniosExperiencia",
                    label: "Años de Experiencia",
                    type: "number",
                  },
                ].map(({ name, label, type = "text" }) => (
                  <div key={name}>
                    <label>{label}:</label>
                    <Field name={name} type={type} className="form-input" />
                    <ErrorMessage
                      name={name}
                      component="div"
                      className="error"
                    />
                  </div>
                ))}
              </div>
            </fieldset>

            {/* Botones de Acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar Profesor
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => handleLimpiar(setValues)}
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

export default ProfesoresFormulario;
