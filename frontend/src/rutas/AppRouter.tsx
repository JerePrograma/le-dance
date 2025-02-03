/***********************************************
 * src/rutas/AppRouter.tsx
 ***********************************************/
import { lazy, Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import Encabezado from "../componentes/comunes/Encabezado";
import ProtectedRoute from "./ProtectedRoute";

// 🔹 Páginas con carga diferida
const Login = lazy(() => import("../paginas/Login"));
const Inicio = lazy(() => import("../paginas/Inicio"));
const Reportes = lazy(() => import("../paginas/Reportes")); // 🔹 ¡Agregamos la página de reportes!

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

const AppRouter = () => {
  return (
    <>
      <Encabezado />

      <Suspense fallback={<div>Cargando...</div>}>
        <Routes>
          {/* 🔹 Rutas públicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<FormularioUsuarios />} />

          {/* 🔹 Rutas protegidas */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Inicio />} />
            <Route path="/reportes" element={<Reportes />} />{" "}
            {/* ✅ ¡RUTA DE REPORTES! */}
            {/* 🔹 Usuarios */}
            <Route path="/usuarios" element={<Usuarios />} />
            <Route
              path="/usuarios/formulario"
              element={<FormularioUsuarios />}
            />
            {/* 🔹 Profesores */}
            <Route path="/profesores" element={<Profesores />} />
            <Route
              path="/profesores/formulario"
              element={<FormularioProfesores />}
            />
            {/* 🔹 Disciplinas */}
            <Route path="/disciplinas" element={<Disciplinas />} />
            <Route
              path="/disciplinas/formulario"
              element={<FormularioDisciplinas />}
            />
            {/* 🔹 Alumnos */}
            <Route path="/alumnos" element={<Alumnos />} />
            <Route path="/alumnos/formulario" element={<FormularioAlumnos />} />
            {/* 🔹 Asistencias */}
            <Route path="/asistencias" element={<Asistencias />} />
            <Route
              path="/asistencias/formulario"
              element={<FormularioAsistencias />}
            />
            {/* 🔹 Bonificaciones */}
            <Route path="/bonificaciones" element={<Bonificaciones />} />
            <Route
              path="/bonificaciones/formulario"
              element={<FormularioBonificaciones />}
            />
            {/* 🔹 Roles */}
            <Route path="/roles" element={<Roles />} />
            <Route path="/roles/formulario" element={<FormularioRoles />} />
            {/* 🔹 Inscripciones */}
            <Route path="/inscripciones" element={<Inscripciones />} />
            <Route
              path="/inscripciones/formulario"
              element={<FormularioInscripciones />}
            />
          </Route>
        </Routes>
      </Suspense>
    </>
  );
};

export default AppRouter;
