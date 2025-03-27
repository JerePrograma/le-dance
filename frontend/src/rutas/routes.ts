// AppRouter.tsx
import { lazy } from "react";

// 📌 Rutas públicas
const Login = lazy(() => import("../paginas/Login"));
const Registro = lazy(
  () => import("../funcionalidades/usuarios/UsuariosFormulario")
);
const Unauthorized = lazy(() => import("../paginas/Unauthorized"));

export const publicRoutes = [
  { path: "/login", Component: Login },
  { path: "/registro", Component: Registro },
  { path: "/unauthorized", Component: Unauthorized },
];

// 📌 Rutas protegidas generales
const Dashboard = lazy(() => import("../paginas/Dashboard"));
const Reportes = lazy(() => import("../paginas/Reportes"));

export const protectedRoutes = [
  { path: "/", Component: Dashboard },
  { path: "/reportes", Component: Reportes },
];

// 📌 Rutas solo para ADMINISTRADOR
const UsuariosPagina = lazy(
  () => import("../funcionalidades/usuarios/UsuariosPagina")
);
const UsuariosFormulario = lazy(
  () => import("../funcionalidades/usuarios/UsuariosFormulario")
);
const RolesPagina = lazy(() => import("../funcionalidades/roles/RolesPagina"));
const RolesFormulario = lazy(
  () => import("../funcionalidades/roles/RolesFormulario")
);

export const adminRoutes = [
  { path: "/usuarios", Component: UsuariosPagina },
  { path: "/usuarios/formulario", Component: UsuariosFormulario },
  { path: "/roles", Component: RolesPagina },
  { path: "/roles/formulario", Component: RolesFormulario },
];

// 📌 Otras rutas protegidas
const ProfesoresPagina = lazy(
  () => import("../funcionalidades/profesores/ProfesoresPagina")
);
const ProfesoresFormulario = lazy(
  () => import("../funcionalidades/profesores/ProfesoresFormulario")
);
const DisciplinasPagina = lazy(
  () => import("../funcionalidades/disciplinas/DisciplinasPagina")
);
const DisciplinasFormulario = lazy(
  () => import("../funcionalidades/disciplinas/DisciplinasFormulario")
);
const AlumnosPagina = lazy(
  () => import("../funcionalidades/alumnos/AlumnosPagina")
);
const AlumnosFormulario = lazy(
  () => import("../funcionalidades/alumnos/AlumnosFormulario")
);
const SalonesPagina = lazy(
  () => import("../funcionalidades/salones/SalonesPagina")
);
const SalonesFormulario = lazy(
  () => import("../funcionalidades/salones/SalonesFormulario")
);
const BonificacionesPagina = lazy(
  () => import("../funcionalidades/bonificaciones/BonificacionesPagina")
);
const BonificacionesFormulario = lazy(
  () => import("../funcionalidades/bonificaciones/BonificacionesFormulario")
);
const InscripcionesPagina = lazy(
  () => import("../funcionalidades/inscripciones/InscripcionesPagina")
);
const InscripcionesFormulario = lazy(
  () => import("../funcionalidades/inscripciones/InscripcionesFormulario")
);
const AsistenciaDiariaFormulario = lazy(
  () =>
    import("../funcionalidades/asistencias-diarias/AsistenciaDiariaFormulario")
);
const AsistenciaMensualDetalle = lazy(
  () =>
    import("../funcionalidades/asistencias-mensuales/AsistenciaMensualDetalle")
);
const PagosPagina = lazy(() => import("../funcionalidades/pagos/PagosPagina"));
const EditarPagoForm = lazy(
  () => import("../funcionalidades/pagos/EditarPagoForm.tsx")
);
const PagosFormulario = lazy(
  () => import("../funcionalidades/pagos/PagosFormulario")
);
const CajaPagina = lazy(() => import("../funcionalidades/caja/CajaPagina"));
const ConsultaObservacionesProfesores = lazy(
  () =>
    import(
      "../funcionalidades/observaciones/ConsultaObservacionesProfesores.tsx"
    )
);
const PagosPendientes = lazy(
  () => import("../funcionalidades/caja/PagosPendientes")
);
const PagosCobrados = lazy(
  () => import("../funcionalidades/caja/PagosCobrados")
);
const EgresoDebitoPagina = lazy(
  () => import("../funcionalidades/caja/EgresoDebitoPagina")
);
const CajaFormulario = lazy(
  () => import("../funcionalidades/caja/CajaFormulario")
);
const CobranzaPagina = lazy(
  () => import("../funcionalidades/pagos/CobranzaPagina")
);
const PagosAlumnoPagina = lazy(
  () => import("../funcionalidades/pagos/PagosAlumnoPagina")
);
const StocksPagina = lazy(
  () => import("../funcionalidades/stock/StocksPagina")
);
const StocksFormulario = lazy(
  () => import("../funcionalidades/stock/StocksFormulario")
);
const ConceptosPagina = lazy(
  () => import("../funcionalidades/conceptos/ConceptosPagina")
);
const ConceptosFormulario = lazy(
  () => import("../funcionalidades/conceptos/ConceptosFormulario")
);
const MetodosPagoPagina = lazy(
  () => import("../funcionalidades/metodos-pago/MetodosPagoPagina")
);
const MensualidadPagina = lazy(
  () => import("../funcionalidades/mensualidades/MensualidadPagina.tsx")
);
const MetodosPagoFormulario = lazy(
  () => import("../funcionalidades/metodos-pago/MetodosPagoFormulario")
);
const PlanillaCajaGeneral = lazy(
  () => import("../funcionalidades/caja/PlanillaCajaGeneral")
);
const ConsultaCajaDiaria = lazy(
  () => import("../funcionalidades/caja/ConsultaCajaDiaria")
);
const RendicionMensual = lazy(
  () => import("../funcionalidades/caja/RendicionMensual")
);
const RecargosPagina = lazy(
  () => import("../funcionalidades/recargos/RecargosPagina")
);
const RecargosFormulario = lazy(
  () => import("../funcionalidades/recargos/RecargosFormulario")
);
const ReporteDetallePago = lazy(
  () => import("../funcionalidades/reportes/ReporteDetallePago")
);
const SubConceptosPagina = lazy(
  () => import("../funcionalidades/subconceptos/SubConceptosPagina")
);
const SubConceptosFormulario = lazy(
  () => import("../funcionalidades/subconceptos/SubConceptosFormulario")
);

export const otherProtectedRoutes = [
  { path: "/profesores", Component: ProfesoresPagina },
  { path: "/profesores/formulario", Component: ProfesoresFormulario },
  { path: "/disciplinas", Component: DisciplinasPagina },
  { path: "/disciplinas/formulario", Component: DisciplinasFormulario },
  { path: "/alumnos", Component: AlumnosPagina },
  { path: "/alumnos/formulario", Component: AlumnosFormulario },
  { path: "/salones", Component: SalonesPagina },
  { path: "/salones/formulario", Component: SalonesFormulario },
  { path: "/bonificaciones", Component: BonificacionesPagina },
  { path: "/bonificaciones/formulario", Component: BonificacionesFormulario },
  { path: "/inscripciones", Component: InscripcionesPagina },
  { path: "/inscripciones/formulario", Component: InscripcionesFormulario },
  { path: "/asistencias/alumnos", Component: AsistenciaDiariaFormulario },
  { path: "/asistencias-mensuales", Component: AsistenciaMensualDetalle },
  { path: "/pagos", Component: PagosPagina },
  { path: "/pagos/editar", Component: EditarPagoForm },
  { path: "/pagos/formulario", Component: PagosFormulario },
  { path: "/caja", Component: CajaPagina },
  {
    path: "/observaciones-profesores",
    Component: ConsultaObservacionesProfesores,
  },
  { path: "/pagos-pendientes", Component: PagosPendientes },
  { path: "/pagos-cobrados", Component: PagosCobrados },
  { path: "/debitos", Component: EgresoDebitoPagina },
  { path: "/caja/formulario", Component: CajaFormulario },
  { path: "/cobranza/:alumnoId", Component: CobranzaPagina },
  { path: "/pagos/alumno/:alumnoId", Component: PagosAlumnoPagina },
  { path: "/stocks", Component: StocksPagina },
  { path: "/stocks/formulario", Component: StocksFormulario },
  { path: "/conceptos", Component: ConceptosPagina },
  { path: "/conceptos/formulario-concepto", Component: ConceptosFormulario },
  { path: "/metodos-pago", Component: MetodosPagoPagina },
  { path: "/mensualidades", Component: MensualidadPagina },
  { path: "/metodos-pago/formulario", Component: MetodosPagoFormulario },
  { path: "/caja/planilla", Component: PlanillaCajaGeneral },
  { path: "/caja/diaria", Component: ConsultaCajaDiaria },
  { path: "/caja/rendicion-mensual", Component: RendicionMensual },
  { path: "/recargos", Component: RecargosPagina },
  { path: "/recargos/formulario", Component: RecargosFormulario },
  { path: "/liquidacion", Component: ReporteDetallePago },
  { path: "/subconceptos", Component: SubConceptosPagina },
  { path: "/subconceptos/formulario", Component: SubConceptosFormulario },
];
