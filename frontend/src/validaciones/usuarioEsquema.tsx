import * as Yup from "yup";

export const usuarioEsquema = Yup.object().shape({
  email: Yup.string()
    .email("Debe ser un email válido")
    .required("El email es obligatorio"),
  nombreUsuario: Yup.string()
    .min(3, "El nombre de usuario debe tener al menos 3 caracteres")
    .required("El nombre de usuario es obligatorio"),
  contrasena: Yup.string()
    .min(6, "La contraseña debe tener al menos 6 caracteres")
    .required("La contraseña es obligatoria"),
  rol: Yup.string().required("El rol es obligatorio"),
});
