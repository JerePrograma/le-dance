import { lazy, Suspense } from "react";
import { createRoutesFromElements, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";

// 🔹 Páginas con carga diferida
const Login = lazy(() => import("../paginas/Login"));
const Inicio = lazy(() => import("../paginas/Inicio"));
const Reportes = lazy(() => import("../paginas/Reportes"));

// Usuarios
const Usuarios = lazy(
  () => import("../funcionalidades/usuarios/UsuariosPagina")
);
const FormularioUsuarios = lazy(
  () => import("../funcionalidades/usuarios/UsuariosFormulario")
);

// Profesores
const Profesores = lazy(
  () => import("../funcionalidades/profesores/ProfesoresPagina")
);
const FormularioProfesores = lazy(
  () => import("../funcionalidades/profesores/ProfesoresFormulario")
);

// Disciplinas
const Disciplinas = lazy(
  () => import("../funcionalidades/disciplinas/DisciplinasPagina")
);
const FormularioDisciplinas = lazy(
  () => import("../funcionalidades/disciplinas/DisciplinasFormulario")
);

// Alumnos
const Alumnos = lazy(() => import("../funcionalidades/alumnos/AlumnosPagina"));
const FormularioAlumnos = lazy(
  () => import("../funcionalidades/alumnos/AlumnosFormulario")
);

// Asistencias
const Asistencias = lazy(
  () => import("../funcionalidades/asistencias/AsistenciasPagina")
);
const FormularioAsistencias = lazy(
  () => import("../funcionalidades/asistencias/AsistenciasFormulario")
);

// Bonificaciones
const Bonificaciones = lazy(
  () => import("../funcionalidades/bonificaciones/BonificacionesPagina")
);
const FormularioBonificaciones = lazy(
  () => import("../funcionalidades/bonificaciones/BonificacionesFormulario")
);

// Roles
const Roles = lazy(() => import("../funcionalidades/roles/RolesPagina"));
const FormularioRoles = lazy(
  () => import("../funcionalidades/roles/RolesFormulario")
);

// Inscripciones
const Inscripciones = lazy(
  () => import("../funcionalidades/inscripciones/InscripcionesPagina")
);
const FormularioInscripciones = lazy(
  () => import("../funcionalidades/inscripciones/InscripcionesFormulario")
);

const routes = createRoutesFromElements(
  <Suspense fallback={<div>Cargando...</div>}>
    <Route path="/login" element={<Login />} />
    <Route path="/registro" element={<FormularioUsuarios />} />

    <Route element={<ProtectedRoute />}>
      <Route path="/" element={<Inicio />} />
      <Route path="/reportes" element={<Reportes />} />
      <Route path="/usuarios" element={<Usuarios />} />
      <Route path="/usuarios/formulario" element={<FormularioUsuarios />} />
      <Route path="/profesores" element={<Profesores />} />
      <Route path="/profesores/formulario" element={<FormularioProfesores />} />
      <Route path="/disciplinas" element={<Disciplinas />} />
      <Route
        path="/disciplinas/formulario"
        element={<FormularioDisciplinas />}
      />
      <Route path="/alumnos" element={<Alumnos />} />
      <Route path="/alumnos/formulario" element={<FormularioAlumnos />} />
      <Route path="/asistencias" element={<Asistencias />} />
      <Route
        path="/asistencias/formulario"
        element={<FormularioAsistencias />}
      />
      <Route path="/bonificaciones" element={<Bonificaciones />} />
      <Route
        path="/bonificaciones/formulario"
        element={<FormularioBonificaciones />}
      />
      <Route path="/roles" element={<Roles />} />
      <Route path="/roles/formulario" element={<FormularioRoles />} />
      <Route path="/inscripciones" element={<Inscripciones />} />
      <Route
        path="/inscripciones/formulario"
        element={<FormularioInscripciones />}
      />
    </Route>
  </Suspense>
);

export default routes;
