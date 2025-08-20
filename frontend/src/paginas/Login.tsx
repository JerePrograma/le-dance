// Login.tsx
import { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../hooks/context/authContext";
import Boton from "../componentes/comunes/Boton";
import { Form, Formik, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { prefetch } from "../rutas/routes";

const loginSchema = Yup.object().shape({
  nombreUsuario: Yup.string().required("Nombre de Usuario es requerido"),
  contrasena: Yup.string().required("Contraseña es requerida"),
});

const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");

  // Prefetch “en idle” del Dashboard (posible siguiente pantalla)
  useEffect(() => {
    const id = (window as any).requestIdleCallback
      ? (window as any).requestIdleCallback(() => prefetch.dashboard())
      : setTimeout(() => prefetch.dashboard(), 500);
    return () => (window as any).cancelIdleCallback?.(id) ?? clearTimeout(id);
  }, []);

  const handleLogin = async (values: {
    nombreUsuario: string;
    contrasena: string;
  }) => {
    try {
      await login(values.nombreUsuario, values.contrasena);
      navigate("/");
    } catch {
      setError("Credenciales incorrectas. Intenta nuevamente.");
      // carga perezosa del bundle de notificaciones
      const { toast } = await import("react-toastify");
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
        <Link
          to="/registro"
          className="text-primary hover:underline"
          onMouseEnter={prefetch.registro} // prefetch en hover
          onFocus={prefetch.registro} // accesibilidad
        >
          ¿No tienes cuenta? Regístrate aquí
        </Link>
      </div>
    </div>
  );
};

export default Login;
