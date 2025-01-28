/***********************************************
 * src/rutas/AppRouter.tsx
 ***********************************************/
import { Routes, Route } from "react-router-dom";
import Encabezado from "../componentes/comunes/Encabezado";
import ProtectedRoute from "./ProtectedRoute";
import Login from "../paginas/Login";
import Inicio from "../paginas/Inicio";
import FormularioUsuarios from "../funcionalidades/usuarios/UsuariosFormulario";
import FormularioAlumnos from "../funcionalidades/alumnos/AlumnosFormulario";
import FormularioAsistencias from "../funcionalidades/asistencias/AsistenciasFormulario";
import FormularioProfesores from "../funcionalidades/profesores/ProfesoresFormulario";
import FormularioDisciplinas from "../funcionalidades/disciplinas/DisciplinasFormulario";
import FormularioBonificaciones from "../funcionalidades/bonificaciones/BonificacionesFormulario";
import FormularioInscripciones from "../funcionalidades/inscripciones/InscripcionesFormulario";
import FormularioRoles from "../funcionalidades/roles/RolesFormulario";
import Usuarios from "../funcionalidades/usuarios/UsuariosPagina";
import Alumnos from "../funcionalidades/alumnos/AlumnosPagina";
import Asistencias from "../funcionalidades/asistencias/AsistenciasPagina";
import Disciplinas from "../funcionalidades/disciplinas/DisciplinasPagina";
import Profesores from "../funcionalidades/profesores/ProfesoresPagina";
import Bonificaciones from "../funcionalidades/bonificaciones/BonificacionesPagina";
import Roles from "../funcionalidades/roles/RolesPagina";

const AppRouter = () => {
  return (
    <>
      <Encabezado />

      <Routes>
        {/* Rutas públicas */}
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<FormularioUsuarios />} />

        {/* Rutas protegidas (ProtectedRoute) */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/usuarios" element={<Usuarios />} />
          <Route path="/usuarios/formulario" element={<FormularioUsuarios />} />

          <Route path="/profesores" element={<Profesores />} />
          <Route
            path="/profesores/formulario"
            element={<FormularioProfesores />}
          />

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
          <Route
            path="/inscripciones/formulario"
            element={<FormularioInscripciones />}
          />
        </Route>
      </Routes>
    </>
  );
};

export default AppRouter;
