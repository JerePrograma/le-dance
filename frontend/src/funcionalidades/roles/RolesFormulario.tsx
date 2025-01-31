import React, { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { rolEsquema } from "../../validaciones/rolEsquema";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

interface Rol {
  id?: number;
  descripcion: string;
}

const RolesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const initialValues: Rol = {
    descripcion: "",
  };

  const handleBuscar = useCallback(async (idStr: string, setValues: any) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID inválido");
        return;
      }
      const response = await api.get<Rol>(`/api/roles/${idNum}`);
      setValues(response.data);
      toast.success("Rol cargado correctamente.");
    } catch {
      toast.error("Error al cargar los datos del rol.");
      setValues(initialValues);
    }
  }, []);

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) handleBuscar(id, () => {});
  }, [searchParams, handleBuscar]);

  const handleGuardarRol = async (values: Rol) => {
    try {
      if (searchParams.get("id")) {
        await api.put(`/api/roles/${searchParams.get("id")}`, values);
        toast.success("Rol actualizado correctamente.");
      } else {
        await api.post("/api/roles", values);
        toast.success("Rol creado correctamente.");
      }
    } catch {
      toast.error("Error al guardar los datos del rol.");
    }
  };

  return (
    <div className="formulario">
      <h1 className="form-title">
        {searchParams.get("id") ? "Editar Rol" : "Nuevo Rol"}
      </h1>

      <Formik
        initialValues={initialValues}
        validationSchema={rolEsquema}
        onSubmit={handleGuardarRol}
      >
        {({ setValues, isSubmitting }) => (
          <Form className="formulario">
            {/* Búsqueda por ID */}
            <div className="form-busqueda">
              <label htmlFor="idBusqueda">Número de Rol:</label>
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

            {/* Campo de Descripción */}
            <fieldset className="form-fieldset">
              <legend>Información del Rol</legend>
              <div className="form-grid">
                <div>
                  <label>Descripción:</label>
                  <Field
                    name="descripcion"
                    type="text"
                    className="form-input"
                  />
                  <ErrorMessage
                    name="descripcion"
                    component="div"
                    className="error"
                  />
                </div>
              </div>
            </fieldset>

            {/* Botones de Acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar Rol
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => setValues(initialValues)}
              >
                Limpiar
              </Boton>
              <Boton type="button" secondary onClick={() => navigate("/roles")}>
                Volver al Listado
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default RolesFormulario;
