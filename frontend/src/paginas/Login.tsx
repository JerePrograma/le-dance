import { useState } from "react";
import { useAuth } from "../hooks/context/authContext";
import Boton from "../componentes/comunes/Boton";
import { Form, Formik, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";

const loginSchema = Yup.object().shape({
  email: Yup.string().email("Email inválido").required("Email es requerido"),
  contrasena: Yup.string().required("Contraseña es requerida"),
});

const Login: React.FC = () => {
  const { login } = useAuth();
  const [error, setError] = useState("");

  const handleLogin = async (values: { email: string; contrasena: string }) => {
    try {
      await login(values.email, values.contrasena);
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
        initialValues={{ email: "", contrasena: "" }}
        validationSchema={loginSchema}
        onSubmit={handleLogin}
      >
        {({ isSubmitting }) => (
          <Form className="formulario max-w-md mx-auto">
            <div className="mb-4">
              <label htmlFor="email" className="auth-label">
                Email:
              </label>
              <Field
                type="email"
                id="email"
                name="email"
                className="form-input"
                placeholder="Ingrese su email"
              />
              <ErrorMessage
                name="email"
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
