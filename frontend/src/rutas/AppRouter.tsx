// src/rutas/AppRouter.tsx
import { lazy, Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import Encabezado from "../componentes/comunes/Encabezado";
import ProtectedRoute from "./ProtectedRoute";

// Páginas principales (públicas)
const Login = lazy(() => import("../paginas/Login"));
const Inicio = lazy(() => import("../paginas/Dashboard"));
const Reportes = lazy(() => import("../paginas/Reportes"));

// Gestión de asistencias
const AsistenciaDiariaFormulario = lazy(() =>
  import("../funcionalidades/asistencias-diarias/AsistenciasDiariasFormulario")
);
const AsistenciasMensualesListado = lazy(() =>
  import("../funcionalidades/asistencias-mensuales/AsistenciasMensualesListado")
);
const AsistenciaMensualDetalle = lazy(() =>
  import("../funcionalidades/asistencias-mensuales/AsistenciaMensualDetalle")
);
const AsistenciasMensualesFormulario = lazy(() =>
  import("../funcionalidades/asistencias-mensuales/AsistenciasMensualesFormulario")
);
const AsistenciasSeleccion = lazy(() =>
  import("../funcionalidades/asistencias/AsistenciasSeleccion")
);

// Gestión de usuarios y roles
const Usuarios = lazy(() => import("../funcionalidades/usuarios/UsuariosPagina"));
const FormularioUsuarios = lazy(() => import("../funcionalidades/usuarios/UsuariosFormulario"));
const Roles = lazy(() => import("../funcionalidades/roles/RolesPagina"));
const FormularioRoles = lazy(() => import("../funcionalidades/roles/RolesFormulario"));

// Gestión de profesores y disciplinas
const Profesores = lazy(() => import("../funcionalidades/profesores/ProfesoresPagina"));
const FormularioProfesores = lazy(() => import("../funcionalidades/profesores/ProfesoresFormulario"));
const Disciplinas = lazy(() => import("../funcionalidades/disciplinas/DisciplinasPagina"));
const FormularioDisciplinas = lazy(() => import("../funcionalidades/disciplinas/DisciplinasFormulario"));

// Gestión de alumnos y salones
const Alumnos = lazy(() => import("../funcionalidades/alumnos/AlumnosPagina"));
const FormularioAlumnos = lazy(() => import("../funcionalidades/alumnos/AlumnosFormulario"));
const Salones = lazy(() => import("../funcionalidades/salones/SalonesPagina"));
const FormularioSalones = lazy(() => import("../funcionalidades/salones/SalonesFormulario"));

// Gestión de bonificaciones e inscripciones
const Bonificaciones = lazy(() => import("../funcionalidades/bonificaciones/BonificacionesPagina"));
const FormularioBonificaciones = lazy(() => import("../funcionalidades/bonificaciones/BonificacionesFormulario"));
const Inscripciones = lazy(() => import("../funcionalidades/inscripciones/InscripcionesPagina"));
const FormularioInscripciones = lazy(() => import("../funcionalidades/inscripciones/InscripcionesFormulario"));

// (Opcional) Gestión de pagos, si es necesario
// const Pagos = lazy(() => import("../funcionalidades/pagos/PagosPagina"));

const AppRouter = () => {
  return (
    <>
      <Encabezado />
      <Suspense fallback={<div>Cargando...</div>}>
        <Routes>
          {/* Rutas públicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<FormularioUsuarios />} />

          {/* Rutas protegidas */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Inicio />} />
            <Route path="/reportes" element={<Reportes />} />

            {/* Gestión de usuarios y roles */}
            <Route path="/usuarios" element={<Usuarios />} />
            <Route path="/usuarios/formulario" element={<FormularioUsuarios />} />
            <Route path="/roles" element={<Roles />} />
            <Route path="/roles/formulario" element={<FormularioRoles />} />

            {/* Gestión de profesores y disciplinas */}
            <Route path="/profesores" element={<Profesores />} />
            <Route path="/profesores/formulario" element={<FormularioProfesores />} />
            <Route path="/disciplinas" element={<Disciplinas />} />
            <Route path="/disciplinas/formulario" element={<FormularioDisciplinas />} />

            {/* Gestión de alumnos y salones */}
            <Route path="/alumnos" element={<Alumnos />} />
            <Route path="/alumnos/formulario" element={<FormularioAlumnos />} />
            <Route path="/salones" element={<Salones />} />
            <Route path="/salones/formulario" element={<FormularioSalones />} />

            {/* Gestión de bonificaciones e inscripciones */}
            <Route path="/bonificaciones" element={<Bonificaciones />} />
            <Route path="/bonificaciones/formulario" element={<FormularioBonificaciones />} />
            <Route path="/inscripciones" element={<Inscripciones />} />
            <Route path="/inscripciones/formulario" element={<FormularioInscripciones />} />

            {/* Gestión de asistencias */}
            {/* Pantalla de selección */}
            <Route path="/asistencias" element={<AsistenciasSeleccion />} />
            {/* Formulario de registro de asistencias mensuales */}
            <Route path="/asistencias-mensuales/formulario" element={<AsistenciasMensualesFormulario />} />
            {/* Listado de asistencias mensuales */}
            <Route path="/asistencias-mensuales" element={<AsistenciasMensualesListado />} />
            {/* Detalle de asistencia mensual */}
            <Route path="/asistencias-mensuales/:id" element={<AsistenciaMensualDetalle />} />
            {/* Listado de asistencias diarias (sin id) */}
            <Route path="/asistencias-diarias" element={<AsistenciasMensualesListado />} />
            {/* Formulario para modificar asistencia diaria (por alumno) */}
            <Route path="/asistencias-diarias/:id" element={<AsistenciaDiariaFormulario />} />
          </Route>
        </Routes>
      </Suspense>
    </>
  );
};

export default AppRouter;
