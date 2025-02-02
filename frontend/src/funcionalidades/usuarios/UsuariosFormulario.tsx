import type React from "react";
import { useEffect, useState } from "react";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { usuarioEsquema } from "../../validaciones/usuarioEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";

interface Rol {
  id: number;
  descripcion: string;
}

const initialUserValues = {
  email: "",
  nombreUsuario: "",
  contrasena: "",
  rol: "",
};

const UsuariosFormulario: React.FC = () => {
  const [roles, setRoles] = useState<Rol[]>([]);

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const response = await api.get<Rol[]>("/api/roles");
        setRoles(response.data);
      } catch (err) {
        toast.error("Error al cargar los roles.");
      }
    };
    fetchRoles();
  }, []);

  const handleRegistro = async (values: typeof initialUserValues) => {
    try {
      await api.post("/api/usuarios/registro", values);
      toast.success("Usuario registrado correctamente.");
      window.location.href = "/login";
    } catch {
      toast.error("Error al registrar el usuario. Verifica los datos.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Usuario</h1>
      <Formik
        initialValues={initialUserValues}
        validationSchema={usuarioEsquema}
        onSubmit={handleRegistro}
      >
        {({ isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="mb-4">
                <label htmlFor="email" className="auth-label">
                  Email:
                </label>
                <Field
                  type="email"
                  name="email"
                  id="email"
                  className="form-input"
                />
                <ErrorMessage
                  name="email"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="nombreUsuario" className="auth-label">
                  Nombre de Usuario:
                </label>
                <Field
                  type="text"
                  name="nombreUsuario"
                  id="nombreUsuario"
                  className="form-input"
                />
                <ErrorMessage
                  name="nombreUsuario"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="contrasena" className="auth-label">
                  Contraseña:
                </label>
                <Field
                  type="password"
                  name="contrasena"
                  id="contrasena"
                  className="form-input"
                />
                <ErrorMessage
                  name="contrasena"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="rol" className="auth-label">
                  Rol:
                </label>
                <Field as="select" name="rol" id="rol" className="form-input">
                  <option value="">Seleccione un rol</option>
                  {roles.map((rol) => (
                    <option key={rol.id} value={rol.descripcion}>
                      {rol.descripcion}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="rol"
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
                Registrarse
              </Boton>
              <Boton type="reset" className="page-button-secondary">
                Limpiar
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
      <div className="text-center mt-4">
        <a href="/login" className="auth-link">
          ¿Ya tienes cuenta? Inicia sesión aquí
        </a>
      </div>
    </div>
  );
};

export default UsuariosFormulario;
