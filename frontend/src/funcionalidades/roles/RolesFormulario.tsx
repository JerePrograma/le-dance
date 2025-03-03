import type React from "react";
import { useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { rolEsquema } from "../../validaciones/rolEsquema";
import api from "../../api/axiosConfig";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import { Search } from "lucide-react";

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

  const handleBuscar = useCallback(
    async (idStr: string, setValues: (vals: Rol) => void) => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID invalido");
          return;
        }
        const response = await api.get<Rol>(`/roles/${idNum}`);
        setValues(response.data);
        toast.success("Rol cargado correctamente.");
      } catch {
        toast.error("Error al cargar los datos del rol.");
        setValues(initialValues);
      }
    },
    []
  );

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) handleBuscar(id, () => { });
  }, [searchParams, handleBuscar]);

  const handleGuardarRol = async (values: Rol) => {
    try {
      if (values.id) {
        await api.put(`/roles/${values.id}`, values);
        toast.success("Rol actualizado correctamente.");
      } else {
        await api.post("/roles", values);
        toast.success("Rol creado correctamente.");
      }
    } catch {
      toast.error("Error al guardar los datos del rol.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">
        {searchParams.get("id") ? "Editar Rol" : "Nuevo Rol"}
      </h1>
      <Formik
        initialValues={initialValues}
        validationSchema={rolEsquema}
        onSubmit={handleGuardarRol}
        enableReinitialize
      >
        {({ resetForm, isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Numero de Rol:
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

              <div className="col-span-full mb-4">
                <label htmlFor="descripcion" className="auth-label">
                  Descripcion:
                </label>
                <Field
                  name="descripcion"
                  type="text"
                  id="descripcion"
                  className="form-input"
                />
                <ErrorMessage
                  name="descripcion"
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
                Guardar Rol
              </Boton>
              <Boton
                type="reset"
                onClick={() => resetForm({ values: initialValues })}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                onClick={() => navigate("/roles")}
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

export default RolesFormulario;
