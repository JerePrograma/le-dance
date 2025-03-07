import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { usuarioEsquema } from "../../validaciones/usuarioEsquema";
import * as Yup from "yup"; // Para construir el esquema vacío
import usuariosApi from "../../api/usuariosApi";
import api from "../../api/axiosConfig"; // Se utiliza para cargar roles
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";

// Interfaz local para el formulario, donde 'activo' es obligatorio
interface UsuarioFormValues {
  nombreUsuario: string;
  contrasena: string;
  rol: string;
  activo: boolean;
}

interface Rol {
  id: number;
  descripcion: string;
}

const initialUserValues: UsuarioFormValues = {
  nombreUsuario: "",
  contrasena: "",
  rol: "",
  activo: true,
};

const UsuariosFormulario: React.FC = () => {
  const [roles, setRoles] = useState<Rol[]>([]);
  const [initialValues, setInitialValues] = useState<UsuarioFormValues>(initialUserValues);
  const [isEditMode, setIsEditMode] = useState(false);
  const [loading, setLoading] = useState(false);

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const userId = searchParams.get("id");

  // Cargar roles (se puede refactorizar a un módulo rolesApi si se desea)
  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const response = await api.get<Rol[]>("/roles");
        setRoles(response.data);
      } catch (err) {
        toast.error("Error al cargar los roles.");
      }
    };
    fetchRoles();
  }, []);

  // Si se recibe un id, cargar los datos del usuario para editar
  useEffect(() => {
    if (userId) {
      const fetchUsuario = async () => {
        try {
          setLoading(true);
          const response = await usuariosApi.obtenerUsuarioPorId(Number(userId));
          setInitialValues({
            nombreUsuario: response.nombreUsuario,
            contrasena: "", // Se deja vacía por seguridad
            rol: response.rol,
            activo: response.activo,
          });
          setIsEditMode(true);
        } catch (err) {
          toast.error("Error al cargar los datos del usuario.");
        } finally {
          setLoading(false);
        }
      };
      fetchUsuario();
    }
  }, [userId]);

  const handleSubmit = async (values: UsuarioFormValues) => {
    if (isEditMode && userId) {
      try {
        await usuariosApi.actualizarUsuario(Number(userId), values);
        toast.success("Usuario actualizado correctamente.");
        navigate("/usuarios");
      } catch (error: any) {
        toast.error("Error al actualizar el usuario. Verifica los datos.");
      }
    } else {
      try {
        // En registro se ignora 'activo'
        const { activo, ...registroValues } = values;
        await usuariosApi.registrarUsuario(registroValues);
        toast.success("Usuario registrado correctamente.");
        navigate("/login");
      } catch (error: any) {
        toast.error("Error al registrar el usuario. Verifica los datos.");
      }
    }
  };

  if (loading) return <div className="text-center py-4">Cargando datos...</div>;

  // Definimos el esquema de validación: si es edición, no validamos nada
  const validationSchema = isEditMode ? Yup.object().shape({}) : usuarioEsquema;

  return (
    <div className="page-container">
      <h1 className="page-title">{isEditMode ? "Editar Usuario" : "Registro de Usuario"}</h1>
      <Formik
        enableReinitialize
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={handleSubmit}
      >
        {({ isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="mb-4">
                <label htmlFor="nombreUsuario" className="auth-label">
                  Nombre de Usuario:
                </label>
                <Field type="text" name="nombreUsuario" id="nombreUsuario" className="form-input" />
                <ErrorMessage name="nombreUsuario" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="contrasena" className="auth-label">
                  {isEditMode
                    ? "Nueva Contraseña (dejar en blanco para mantener la actual)"
                    : "Contraseña:"}
                </label>
                <Field type="password" name="contrasena" id="contrasena" className="form-input" />
                <ErrorMessage name="contrasena" component="div" className="auth-error" />
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
                <ErrorMessage name="rol" component="div" className="auth-error" />
              </div>
            </div>
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting} className="page-button">
                {isEditMode ? "Actualizar Usuario" : "Registrarse"}
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
