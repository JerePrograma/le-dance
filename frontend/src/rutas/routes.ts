import { lazy } from "react";

// 📌 Rutas públicas
export const publicRoutes = [
    { path: "/login", Component: lazy(() => import("../paginas/Login")) },
    { path: "/registro", Component: lazy(() => import("../funcionalidades/usuarios/UsuariosFormulario")) },
    { path: "/unauthorized", Component: lazy(() => import("../paginas/Unauthorized")) },
];

// 📌 Rutas protegidas generales
export const protectedRoutes = [
    { path: "/", Component: lazy(() => import("../paginas/Dashboard")) },
    { path: "/reportes", Component: lazy(() => import("../paginas/Reportes")) },
];

// 📌 Rutas solo para ADMINISTRADOR
export const adminRoutes = [
    { path: "/usuarios", Component: lazy(() => import("../funcionalidades/usuarios/UsuariosPagina")) },
    { path: "/usuarios/formulario", Component: lazy(() => import("../funcionalidades/usuarios/UsuariosFormulario")) },
    { path: "/roles", Component: lazy(() => import("../funcionalidades/roles/RolesPagina")) },
    { path: "/roles/formulario", Component: lazy(() => import("../funcionalidades/roles/RolesFormulario")) },
];

// 📌 Otras rutas protegidas
export const otherProtectedRoutes = [
    // Gestión de profesores y disciplinas
    { path: "/profesores", Component: lazy(() => import("../funcionalidades/profesores/ProfesoresPagina")) },
    { path: "/profesores/formulario", Component: lazy(() => import("../funcionalidades/profesores/ProfesoresFormulario")) },
    { path: "/disciplinas", Component: lazy(() => import("../funcionalidades/disciplinas/DisciplinasPagina")) },
    { path: "/disciplinas/formulario", Component: lazy(() => import("../funcionalidades/disciplinas/DisciplinasFormulario")) },
    // Gestión de alumnos y salones
    { path: "/alumnos", Component: lazy(() => import("../funcionalidades/alumnos/AlumnosPagina")) },
    { path: "/alumnos/formulario", Component: lazy(() => import("../funcionalidades/alumnos/AlumnosFormulario")) },
    { path: "/salones", Component: lazy(() => import("../funcionalidades/salones/SalonesPagina")) },
    { path: "/salones/formulario", Component: lazy(() => import("../funcionalidades/salones/SalonesFormulario")) },
    // Gestión de bonificaciones e inscripciones
    { path: "/bonificaciones", Component: lazy(() => import("../funcionalidades/bonificaciones/BonificacionesPagina")) },
    { path: "/bonificaciones/formulario", Component: lazy(() => import("../funcionalidades/bonificaciones/BonificacionesFormulario")) },
    { path: "/inscripciones", Component: lazy(() => import("../funcionalidades/inscripciones/InscripcionesPagina")) },
    { path: "/inscripciones/formulario", Component: lazy(() => import("../funcionalidades/inscripciones/InscripcionesFormulario")) },
    // Gestión de asistencias
    { path: "/asistencias/alumnos", Component: lazy(() => import("../funcionalidades/asistencias-diarias/AsistenciaDiariaFormulario")) },
    { path: "/asistencias-mensuales", Component: lazy(() => import("../funcionalidades/asistencias-mensuales/AsistenciaMensualDetalle")) },
    // Gestión de pagos y caja
    { path: "/pagos", Component: lazy(() => import("../funcionalidades/pagos/PagosPagina")) },
    { path: "/pagos/formulario", Component: lazy(() => import("../funcionalidades/pagos/PagosFormulario")) },
    { path: "/caja", Component: lazy(() => import("../funcionalidades/caja/CajaPagina")) },
    { path: "/pagos-pendientes", Component: lazy(() => import("../funcionalidades/caja/PagosPendientes")) },
    { path: "/debitos", Component: lazy(() => import("../funcionalidades/caja/EgresoDebitoPagina")) },
    { path: "/caja/formulario", Component: lazy(() => import("../funcionalidades/caja/CajaFormulario")) },
    { path: "/cobranza/:alumnoId", Component: lazy(() => import("../funcionalidades/pagos/CobranzaPagina")) },
    { path: "/pagos/alumno/:alumnoId", Component: lazy(() => import("../funcionalidades/pagos/PagosAlumnoPagina")) },
    // Stocks y Tipo-Stocks
    { path: "/stocks", Component: lazy(() => import("../funcionalidades/stock/StocksPagina")) },
    { path: "/stocks/formulario", Component: lazy(() => import("../funcionalidades/stock/StocksFormulario")) },
    // Conceptos y Métodos de Pago
    { path: "/conceptos", Component: lazy(() => import("../funcionalidades/conceptos/ConceptosPagina")) },
    { path: "/conceptos/formulario-concepto", Component: lazy(() => import("../funcionalidades/conceptos/ConceptosFormulario")) },
    { path: "/metodos-pago", Component: lazy(() => import("../funcionalidades/metodos-pago/MetodosPagoPagina")) },
    { path: "/metodos-pago/formulario", Component: lazy(() => import("../funcionalidades/metodos-pago/MetodosPagoFormulario")) },
    // Rutas de caja
    { path: "/caja/planilla", Component: lazy(() => import("../funcionalidades/caja/PlanillaCajaGeneral")) },
    { path: "/caja/diaria", Component: lazy(() => import("../funcionalidades/caja/ConsultaCajaDiaria")) },
    { path: "/caja/rendicion-mensual", Component: lazy(() => import("../funcionalidades/caja/RendicionMensual")) },
    // Recargos
    { path: "/recargos", Component: lazy(() => import("../funcionalidades/recargos/RecargosPagina")) },
    { path: "/recargos/formulario", Component: lazy(() => import("../funcionalidades/recargos/RecargosFormulario")) },
    // Reporte de liquidación y Subconceptos
    { path: "/liquidacion", Component: lazy(() => import("../funcionalidades/reportes/ReporteDetallePago")) },
    { path: "/subconceptos", Component: lazy(() => import("../funcionalidades/subconceptos/SubConceptosPagina")) },
    { path: "/subconceptos/formulario", Component: lazy(() => import("../funcionalidades/subconceptos/SubConceptosFormulario")) },
];
