import React, { useEffect, useState } from "react";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { usuarioEsquema } from "../../validaciones/usuarioEsquema";
import api from "../../utilidades/axiosConfig";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

interface Rol {
  id: number;
  descripcion: string;
}

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

  const initialValues = {
    email: "",
    nombreUsuario: "",
    contrasena: "",
    rol: "",
  };

  const handleRegistro = async (values: typeof initialValues) => {
    try {
      await api.post("/api/usuarios/registro", values);
      toast.success("Usuario registrado correctamente.");
      window.location.href = "/login";
    } catch {
      toast.error("Error al registrar el usuario. Verifica los datos.");
    }
  };

  return (
    <div className="auth-container">
      <h1 className="auth-title">Registro</h1>

      <Formik
        initialValues={initialValues}
        validationSchema={usuarioEsquema}
        onSubmit={handleRegistro}
      >
        {({ isSubmitting }) => (
          <Form className="auth-form">
            <div>
              <label className="auth-label">Email:</label>
              <Field type="email" name="email" className="auth-input" />
              <ErrorMessage name="email" component="div" className="error" />
            </div>

            <div>
              <label className="auth-label">Nombre de Usuario:</label>
              <Field type="text" name="nombreUsuario" className="auth-input" />
              <ErrorMessage
                name="nombreUsuario"
                component="div"
                className="error"
              />
            </div>

            <div>
              <label className="auth-label">Contraseña:</label>
              <Field type="password" name="contrasena" className="auth-input" />
              <ErrorMessage
                name="contrasena"
                component="div"
                className="error"
              />
            </div>

            <div>
              <label className="auth-label">Rol:</label>
              <Field as="select" name="rol" className="auth-input">
                <option value="">Seleccione un rol</option>
                {roles.map((rol) => (
                  <option key={rol.id} value={rol.descripcion}>
                    {rol.descripcion}
                  </option>
                ))}
              </Field>
              <ErrorMessage name="rol" component="div" className="error" />
            </div>

            {/* Botones de acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Registrarse
              </Boton>
              <Boton type="reset" secondary>
                Limpiar
              </Boton>
            </div>
          </Form>
        )}
      </Formik>

      <a href="/login" className="auth-link">
        ¿Ya tienes cuenta? Inicia sesión aquí
      </a>
    </div>
  );
};

export default UsuariosFormulario;
