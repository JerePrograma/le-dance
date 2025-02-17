import { useState } from "react";
import { useAuth } from "../hooks/context/authContext";
import Boton from "../componentes/comunes/Boton";
import { Form, Formik, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";

const loginSchema = Yup.object().shape({
  nombreUsuario: Yup.string().required("Nombre de Usuario es requerido"),
  contrasena: Yup.string().required("Contraseña es requerida"),
});

const Login: React.FC = () => {
  const { login } = useAuth();
  const [error, setError] = useState("");

  const handleLogin = async (values: { nombreUsuario: string; contrasena: string }) => {
    try {
      await login(values.nombreUsuario, values.contrasena);
      window.location.href = "/";
    } catch (err) {
      setError("Credenciales incorrectas. Intenta nuevamente.");
      toast.error("Error al iniciar sesión.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Iniciar Sesión</h1>
      <Formik
        initialValues={{ nombreUsuario: "", contrasena: "" }}
        validationSchema={loginSchema}
        onSubmit={handleLogin}
      >
        {({ isSubmitting }) => (
          <Form className="formulario max-w-md mx-auto">
            <div className="mb-4">
              <label htmlFor="nombreUsuario" className="auth-label">
                Nombre de Usuario:
              </label>
              <Field
                type="nombreUsuario"
                id="nombreUsuario"
                name="nombreUsuario"
                className="form-input"
                placeholder="Ingrese su Nombre de Usuario"
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
                id="contrasena"
                name="contrasena"
                className="form-input"
                placeholder="Ingrese su contraseña"
              />
              <ErrorMessage
                name="contrasena"
                component="div"
                className="auth-error"
              />
            </div>

            {error && <div className="auth-error mb-4">{error}</div>}

            <div className="form-acciones">
              <Boton
                type="submit"
                disabled={isSubmitting}
                className="page-button w-full"
              >
                Ingresar
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
      <div className="text-center mt-4">
        <a href="/registro" className="text-primary hover:underline">
          ¿No tienes cuenta? Regístrate aquí
        </a>
      </div>
    </div>
  );
};

export default Login;
