import { lazy, Suspense } from "react";
import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";

// Paginas principales (publicas)
const Login = lazy(() => import("../paginas/Login"));
const Inicio = lazy(() => import("../paginas/Dashboard"));
const Reportes = lazy(() => import("../paginas/Reportes"));

// Gestion de asistencias
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

const AsistenciaDiariaFormulario = lazy(() =>
  import("../funcionalidades/asistencias-diarias/AsistenciaDiariaFormulario")
);
// Gestion de usuarios y roles
const Usuarios = lazy(() => import("../funcionalidades/usuarios/UsuariosPagina"));
const FormularioUsuarios = lazy(() =>
  import("../funcionalidades/usuarios/UsuariosFormulario")
);
const Roles = lazy(() => import("../funcionalidades/roles/RolesPagina"));
const FormularioRoles = lazy(() =>
  import("../funcionalidades/roles/RolesFormulario")
);

// Gestion de profesores y disciplinas
const Profesores = lazy(() => import("../funcionalidades/profesores/ProfesoresPagina"));
const FormularioProfesores = lazy(() =>
  import("../funcionalidades/profesores/ProfesoresFormulario")
);
const Disciplinas = lazy(() => import("../funcionalidades/disciplinas/DisciplinasPagina"));
const FormularioDisciplinas = lazy(() =>
  import("../funcionalidades/disciplinas/DisciplinasFormulario")
);

// Gestion de alumnos y salones
const Alumnos = lazy(() => import("../funcionalidades/alumnos/AlumnosPagina"));
const FormularioAlumnos = lazy(() =>
  import("../funcionalidades/alumnos/AlumnosFormulario")
);
const Salones = lazy(() => import("../funcionalidades/salones/SalonesPagina"));
const FormularioSalones = lazy(() =>
  import("../funcionalidades/salones/SalonesFormulario")
);

// Gestion de bonificaciones e inscripciones
const Bonificaciones = lazy(() =>
  import("../funcionalidades/bonificaciones/BonificacionesPagina")
);
const FormularioBonificaciones = lazy(() =>
  import("../funcionalidades/bonificaciones/BonificacionesFormulario")
);
const Inscripciones = lazy(() =>
  import("../funcionalidades/inscripciones/InscripcionesPagina")
);
const FormularioInscripciones = lazy(() =>
  import("../funcionalidades/inscripciones/InscripcionesFormulario")
);

// Gestion de pagos y caja
const Pagos = lazy(() => import("../funcionalidades/pagos/PagosPagina"));
const FormularioPagos = lazy(() => import("../funcionalidades/pagos/PagosFormulario"));
const Caja = lazy(() => import("../funcionalidades/caja/CajaPagina"));
const CajaFormulario = lazy(() => import("../funcionalidades/caja/CajaFormulario")); // Nueva importacion
const RendicionMensual = lazy(() => import("../funcionalidades/caja/RendicionMensual")); // Nueva importacion

// NUEVAS PAGINAS: STOCKS y TIPO-STOCKS
const Stocks = lazy(() => import("../funcionalidades/stock/StocksPagina"));
const FormularioStocks = lazy(() =>
  import("../funcionalidades/stock/StocksFormulario")
);
const TipoStocks = lazy(() =>
  import("../funcionalidades/tipoStocks/TipoStocksPagina")
);
const FormularioTipoStocks = lazy(() =>
  import("../funcionalidades/tipoStocks/TipoStocksFormulario")
);

// NUEVAS PAGINAS: Conceptos
const ConceptosPagina = lazy(() =>
  import("../funcionalidades/conceptos/ConceptosPagina")
);
const ConceptosFormulario = lazy(() =>
  import("../funcionalidades/conceptos/ConceptosFormulario")
);

// NUEVAS PAGINAS: Metodos de Pago (CRUD)
const MetodosPagoPagina = lazy(() =>
  import("../funcionalidades/metodos-pago/MetodosPagoPagina")
);
const MetodosPagoFormulario = lazy(() =>
  import("../funcionalidades/metodos-pago/MetodosPagoFormulario")
);

const PlanillaCajaGeneral = lazy(() =>
  import("../funcionalidades/caja/PlanillaCajaGeneral")
);
const ConsultaCajaDiaria = lazy(() =>
  import("../funcionalidades/caja/ConsultaCajaDiaria")
);

const CobranzaPagina = lazy(() => import("../funcionalidades/pagos/CobranzaPagina"));

// Importaciones de Recargos
const Recargos = lazy(() => import("../funcionalidades/recargos/RecargosPagina"));
const FormularioRecargos = lazy(() =>
  import("../funcionalidades/recargos/RecargosFormulario")
)

const ReporteDetallePago = lazy(() =>
  import("../funcionalidades/reportes/ReporteDetallePago")
)

const SubConceptosPagina = lazy(() =>
  import("../funcionalidades/subconceptos/SubConceptosPagina")
);
const SubConceptosFormulario = lazy(() =>
  import("../funcionalidades/subconceptos/SubConceptosFormulario")
);


const AppRouter = () => {
  return (
    <>
      <Suspense fallback={<div>Cargando...</div>}>
        <Routes>
          {/* Rutas publicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<FormularioUsuarios />} />

          {/* Rutas protegidas */}
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<Inicio />} />
            <Route path="/reportes" element={<Reportes />} />

            {/* Gestion de usuarios y roles */}
            <Route path="/usuarios" element={<Usuarios />} />
            <Route path="/usuarios/formulario" element={<FormularioUsuarios />} />
            <Route path="/roles" element={<Roles />} />
            <Route path="/roles/formulario" element={<FormularioRoles />} />

            {/* Gestion de profesores y disciplinas */}
            <Route path="/profesores" element={<Profesores />} />
            <Route path="/profesores/formulario" element={<FormularioProfesores />} />
            <Route path="/disciplinas" element={<Disciplinas />} />
            <Route path="/disciplinas/formulario" element={<FormularioDisciplinas />} />

            {/* Gestion de alumnos y salones */}
            <Route path="/alumnos" element={<Alumnos />} />
            <Route path="/alumnos/formulario" element={<FormularioAlumnos />} />
            <Route path="/salones" element={<Salones />} />
            <Route path="/salones/formulario" element={<FormularioSalones />} />

            {/* Gestion de bonificaciones e inscripciones */}
            <Route path="/bonificaciones" element={<Bonificaciones />} />
            <Route path="/bonificaciones/formulario" element={<FormularioBonificaciones />} />
            <Route path="/inscripciones" element={<Inscripciones />} />
            <Route path="/inscripciones/formulario" element={<FormularioInscripciones />} />

            {/* Gestion de asistencias */}
            <Route path="/asistencias" element={<AsistenciasSeleccion />} />
            <Route path="/asistencias-mensuales/formulario" element={<AsistenciasMensualesFormulario />} />
            <Route path="/asistencias/alumnos" element={<AsistenciaDiariaFormulario />} />
            <Route path="/asistencias-mensuales" element={<AsistenciaMensualDetalle />} />
            <Route path="/asistencias-diarias" element={<AsistenciasMensualesListado />} />

            {/* Gestion de pagos y caja */}
            <Route path="/pagos" element={<Pagos />} />
            <Route path="/pagos/formulario" element={<FormularioPagos />} />
            <Route path="/caja" element={<Caja />} />
            <Route
              path="/caja/formulario"
              element={<CajaFormulario />}
            />

            {/* NUEVAS RUTAS: STOCKS y TIPO-STOCKS */}
            <Route path="/stocks" element={<Stocks />} />
            <Route path="/stocks/formulario" element={<FormularioStocks />} />
            <Route path="/tipo-stocks" element={<TipoStocks />} />
            <Route path="/tipo-stocks/formulario" element={<FormularioTipoStocks />} />

            {/* Nuevas rutas para Conceptos */}
            <Route path="/conceptos" element={<ConceptosPagina />} />
            <Route path="/conceptos/formulario-concepto" element={<ConceptosFormulario />} />

            {/* NUEVAS RUTAS: Metodos de Pago */}
            <Route path="/metodos-pago" element={<MetodosPagoPagina />} />
            <Route path="/metodos-pago/formulario" element={<MetodosPagoFormulario />} />

            <Route path="/cobranza/:alumnoId" element={<CobranzaPagina />} />

            {/* NUEVAS RUTAS: */}
            <Route path="/caja/planilla" element={<PlanillaCajaGeneral />} />
            <Route path="/caja/diaria" element={<ConsultaCajaDiaria />} />
            <Route path="/caja/rendicion-mensual" element={<RendicionMensual />} />

            {/* Gestion de Recargos */}
            <Route path="/recargos" element={<Recargos />} />
            <Route path="/recargos/formulario" element={<FormularioRecargos />} />


            <Route path="/liquidacion" element={<ReporteDetallePago />} />
            <Route path="/" element={<Inicio />} />

            {/* Rutas para SubConceptos */}
            <Route path="/subconceptos" element={<SubConceptosPagina />} />
            <Route path="/subconceptos/formulario" element={<SubConceptosFormulario />} />

          </Route>
        </Routes>
      </Suspense>
    </>
  );
};

export default AppRouter;
