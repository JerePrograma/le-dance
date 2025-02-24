// ==========================================
// UTILITY TYPES & ENUMS
// ==========================================
export type LocalDate = string;
export type LocalTime = string;

export enum DiaSemana {
  LUNES = "LUNES",
  MARTES = "MARTES",
  MIERCOLES = "MIERCOLES",
  JUEVES = "JUEVES",
  VIERNES = "VIERNES",
  SABADO = "SABADO",
  DOMINGO = "DOMINGO",
}

export enum EstadoInscripcion {
  ACTIVA = "ACTIVA",
  INACTIVA = "INACTIVA",
  FINALIZADA = "FINALIZADA",
}

export enum EstadoAsistencia {
  PRESENTE = "PRESENTE",
  AUSENTE = "AUSENTE",
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ==========================================
// ALUMNO
// ==========================================
export interface AlumnoRegistroRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento: LocalDate;
  fechaIncorporacion: LocalDate;
  celular1?: string;
  celular2?: string;
  email1?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  otrasNotas?: string;
  cuotaTotal?: number;
  disciplinas: InscripcionDisciplinaRequest[];
}

export interface AlumnoModificacionRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento: LocalDate;
  celular1?: string;
  celular2?: string;
  telefono?: string;
  email1?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  otrasNotas?: string;
  cuotaTotal?: number;
  activo: boolean;
  disciplinas: InscripcionDisciplinaRequest[];
}

export interface AlumnoResponse {
  id: number;
  nombre: string;
  apellido: string;
  fechaNacimiento?: string;
  edad?: number;
  celular1?: string;
  celular2?: string;
  email1?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  fechaIncorporacion?: string;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  activo?: boolean;
  otrasNotas?: string;
  cuotaTotal?: number;
  disciplinas: { id: number; nombre: string }[];
}

export interface AlumnoListadoResponse {
  id: number;
  nombre: string;
  apellido: string;
  activo?: boolean;
}

export interface AlumnoDetalleResponse {
  id: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string;
  edad: number;
  celular1?: string;
  celular2?: string;
  telefono?: string;
  email1?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  fechaIncorporacion: string;
  fechaDeBaja?: string;
  deudaPendiente: boolean;
  nombrePadres?: string;
  autorizadoParaSalirSolo: boolean;
  otrasNotas?: string;
  cuotaTotal?: number;
  inscripciones: InscripcionResponse[];
}

export interface Alumno {
  id: number;
  nombre: string;
  apellido: string;
}

// ==========================================
// PROFESOR
// ==========================================
export interface ProfesorRegistroRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento?: LocalDate;
  telefono?: string;
}

export interface ProfesorModificacionRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento?: LocalDate;
  telefono?: string;
  activo: boolean;
}

export interface ProfesorDetalleResponse {
  id: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string;
  edad: number;
  telefono?: string;
  activo: boolean;
  disciplinas: DisciplinaListadoResponse[];
}

export interface ProfesorListadoResponse {
  id: number;
  nombre: string;
  apellido: string;
  activo: boolean;
}

// ==========================================
// DISCIPLINA
// ==========================================
export interface DisciplinaRegistroRequest {
  nombre: string;
  diasSemana: DiaSemana[];
  frecuenciaSemanal?: number;
  horarioInicio: LocalTime;
  duracion: number;
  salonId: number;
  profesorId: number;
  recargoId?: number;
  valorCuota: number;
  matricula: number;
  claseSuelta?: number;
  clasePrueba?: number;
}

export interface DisciplinaModificacionRequest {
  nombre: string;
  diasSemana: DiaSemana[];
  frecuenciaSemanal?: number;
  horarioInicio: LocalTime;
  duracion: number;
  salonId: number;
  profesorId: number;
  recargoId?: number;
  valorCuota: number;
  matricula: number;
  claseSuelta?: number;
  clasePrueba?: number;
  activo: boolean;
}

export interface DisciplinaResponse {
  id: number;
  nombre: string;
  horario: string;
  frecuenciaSemanal: number;
  duracion: string;
  salon: string;
  valorCuota: number;
  matricula: number;
  profesorId: number | null;
  inscritos: number;
  activo?: boolean;
}

export interface DisciplinaListadoResponse {
  id: number;
  nombre: string;
  horarioInicio: string;
  activo: boolean;
  profesorNombre: string;
  claseSuelta: number;
  clasePrueba: number;
  valorCuota: number;
}

export interface DisciplinaDetalleResponse {
  id: number;
  nombre: string;
  diasSemana: string[];
  horarioInicio: string;
  duracion: number;
  salon: string;
  profesorNombre: string;
  profesorApellido: string;
  valorCuota: number;
  matricula: number;
  claseSuelta?: number;
  clasePrueba?: number;
  activo: boolean;
  inscritos: number;
}

// ==========================================
// ASISTENCIA
// ==========================================
export interface AsistenciaDiaria {
  id: number;
  fecha: string;
  estado: EstadoAsistencia;
  alumnoId: number;
  asistenciaMensualId: number;
  observacion?: string;
}

export interface AsistenciaDiariaRegistroRequest {
  id?: number;
  fecha: string;
  estado: EstadoAsistencia;
  alumnoId: number;
  asistenciaMensualId: number;
  observacion?: string;
}

export interface AsistenciaDiariaResponse {
  id: number;
  fecha: string;
  estado: EstadoAsistencia;
  alumnoId: number;
  asistenciaMensualId: number;
  observacion?: string;
}

export interface AsistenciaMensualDetalleRequest {
  id: number;
  disciplina: string;
  mes: number;
  anio: number;
  alumnos: Alumno[];
  asistenciasDiarias: AsistenciaDiaria[];
}

export type AsistenciaMensualRegistroRequest = {
  mes: number;
  anio: number;
  inscripcionId?: number; // Opcional si puede no tener alumnos
  asistenciasDiarias?: AsistenciaDiariaRegistroRequest[];
};

export interface AsistenciaMensualModificacionRequest {
  observaciones: {
    alumnoId: number;
    observacion: string;
  }[];
}

export interface AsistenciaMensualDetalleResponse {
  id: number;
  mes: number;
  anio: number;
  disciplina: string;
  profesor: string;
  asistenciasDiarias: AsistenciaDiariaResponse[];
  observaciones: {
    alumnoId: number;
    observacion: string;
  }[];
  disciplinaId: number;
  inscripcionId: number;
  alumnos: {
    id: number;
    nombre: string;
    apellido: string;
  }[];
}

export interface AsistenciaMensualListadoResponse {
  id: number;
  mes: number;
  anio: number;
  disciplina: string;
  profesor: string;
  cantidadAlumnos: number;
}

// ==========================================
// INSCRIPCIÓN
// ==========================================
export interface InscripcionDisciplinaRequest {
  disciplinaId: number;
  bonificacionId?: number;
}

export interface InscripcionRegistroRequest {
  alumnoId: number;
  inscripcion: InscripcionDisciplinaRequest;
  fechaInscripcion?: string; // Opcional
  notas?: string;
}

export interface InscripcionModificacionRequest {
  alumnoId: number;
  disciplinaId: number;
  bonificacionId?: number;
  fechaBaja?: string; // Cambiado de LocalDate a string
  activo?: boolean;
  costoParticular?: number;
  notas?: string;
  estado?: EstadoInscripcion;
}

export interface InscripcionResponse {
  id: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
  };
  disciplina: {
    id: number;
    nombre: string;
  };
  bonificacion?: {
    id: number;
    descripcion: string;
  };
  notas?: string;
}

// ==========================================
// BONIFICACIÓN
// ==========================================
export interface BonificacionRegistroRequest {
  descripcion: string;
  porcentajeDescuento: number;
  observaciones?: string;
}

export interface BonificacionModificacionRequest {
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

export interface BonificacionResponse {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo?: boolean;
  observaciones?: string;
}

// ==========================================
// PAGO Y MÉTODOS DE PAGO
// ==========================================
export interface PagoModificacionRequest {
  fecha: LocalDate;
  fechaVencimiento: LocalDate;
  monto: number;
  metodoPagoId?: number;
  recargoAplicado?: boolean;
  bonificacionAplicada?: boolean;
  saldoRestante: number;
  activo: boolean;
}

export interface MetodoPagoRegistroRequest {
  descripcion: string;
}

export interface MetodoPagoModificacionRequest {
  descripcion: string;
  activo: boolean;
}

export interface DetallePagoResponse {
  id: number;
  codigoConcepto?: string;
  concepto: string;
  cuota?: string;
  valorBase: number;
  bonificacion: number;
  recargo: number;
  aFavor: number;
  importe: number;
  aCobrar: number;
}

export interface PagoMedioRegistroRequest {
  monto: number;
  metodoPagoId: number;
}

export interface PagoMedioResponse {
  id: number;
  monto: number;
  metodo: MetodoPagoResponse;
}

export interface MetodoPagoResponse {
  id: number;
  descripcion: string;
  activo: boolean;
}

export interface PagoResponse {
  id: number;
  fecha: string;
  fechaVencimiento: string;
  monto: number;
  metodosPago: PagoMedioResponse[]; // Soporta pagos parciales
  recargoAplicado: boolean;
  bonificacionAplicada: number; // Ahora es un monto
  saldoRestante: number;
  saldoAFavor: number; // Nuevo campo para "A Favor"
  activo: boolean;
  inscripcionId: number;
  detallePagos: DetallePagoResponse[];
}

export interface DetallePagoRegistroRequest {
  codigoConcepto?: string;
  concepto: string;
  cuota?: string;
  valorBase: number;
  bonificacion: number;
  recargo: number;
  aFavor: number;
  importe?: number;
  aCobrar?: number;
}

export interface PagoRegistroRequest {
  fecha: string;
  fechaVencimiento: string;
  monto: number;
  inscripcionId: number;
  metodoPagoId?: number;
  recargoAplicado?: boolean;
  bonificacionAplicada: number;
  saldoRestante: number;
  saldoAFavor: number;
  detallePagos: Array<{
    codigoConcepto?: string;
    concepto: string;
    cuota?: string;
    valorBase: number;
    bonificacion: number;
    recargo: number;
    aFavor: number;
    importe?: number;
    aCobrar?: number;
  }>;
  pagoMedios?: any[];
  // Agregamos la propiedad faltante:
  pagoMatricula: boolean;
}

// ==========================================
// CAJA
// ==========================================
export interface CajaRegistroRequest {
  fecha: LocalDate;
  totalEfectivo: number;
  totalTransferencia: number;
  totalTarjeta: number;
  rangoDesdeHasta?: string;
  observaciones?: string;
}

export interface CajaModificacionRequest {
  fecha: LocalDate;
  totalEfectivo: number;
  totalTransferencia: number;
  totalTarjeta: number;
  rangoDesdeHasta?: string;
  observaciones?: string;
  activo: boolean;
}

export interface CajaResponse {
  id: number;
  fecha: LocalDate;
  totalEfectivo: number;
  totalTransferencia: number;
  totalTarjeta: number;
  rangoDesdeHasta: string;
  observaciones: string;
  activo: boolean;
}

// ==========================================
// RECARGOS
// ==========================================
export interface RecargoRegistroRequest {
  descripcion: string;
  detalles: RecargoDetalleRegistroRequest[];
}

export interface RecargoDetalleRegistroRequest {
  diaDesde: number;
  porcentaje: number;
}

export interface RecargoDetalleModificacionRegistroRequest {
  diaDesde: number;
  porcentaje: number;
}

// ==========================================
// SALÓN
// ==========================================
export interface SalonRegistroRequest {
  nombre: string;
  descripcion?: string;
}

export interface SalonModificacionRequest {
  nombre: string;
  descripcion?: string;
}

export interface SalonResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
}

// ==========================================
// STOCK
// ==========================================
export interface StockRegistroRequest {
  nombre: string;
  precio: number;
  tipoStockId?: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
}

export interface StockModificacionRequest {
  nombre: string;
  precio: number;
  tipoStockId?: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  activo: boolean;
}

export interface StockResponse {
  id: number;
  nombre: string;
  precio: number;
  tipo: TipoStockResponse; // se espera un objeto
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  activo: boolean;
}
// ==========================================
// TIPO STOCK
// ==========================================
export interface TipoStockRegistroRequest {
  descripcion: string;
}

export interface TipoStockModificacionRequest {
  descripcion: string;
  activo: boolean;
}

export interface TipoStockResponse {
  id: number;
  descripcion: string;
  activo: boolean;
}

// ==========================================
// USUARIO Y ROLES
// ==========================================
export interface UsuarioRegistroRequest {
  nombreUsuario: string;
  contrasena: string;
  rol: string;
}

export interface UsuarioModificacionRequest {
  nombreUsuario: string;
  activo: boolean;
}

export interface RolRegistroRequest {
  descripcion: string;
}

export interface RolModificacionRequest {
  descripcion: string;
  activo: boolean;
}

// ==========================================
// REPORTES
// ==========================================
export interface ReporteRegistroRequest {
  tipo: string;
  descripcion: string;
  usuarioId?: number;
}

export interface ReporteModificacionRequest {
  descripcion: string;
  activo: boolean;
}

export interface ReporteResponse {
  id: number;
  tipo: string;
  descripcion: string;
  fechaGeneracion: string; // O LocalDate, según prefieras
  usuarioId?: number;
  activo: boolean;
}

// src/types/conceptosTypes.ts
export interface ConceptoRegistroRequest {
  descripcion: string;
  precio: number;
  subConceptoId: number;
}

export interface ConceptoModificacionRequest {
  descripcion: string;
  precio: number;
  subConceptoId: number;
  activo: boolean;
}

export interface ConceptoResponse {
  id: number;
  descripcion: string;
  precio: number;
  subConcepto: SubConceptoResponse;
}

export interface SubConceptoResponse {
  id: number;
  descripcion: string;
}

export interface MatriculaRegistroRequest {
  alumnoId: number;
  anio: number;
  valor: number;
}

export interface MatriculaModificacionRequest {
  pagada: boolean;
  fechaPago: string; // Formato YYYY-MM-DD
}

export interface MatriculaResponse {
  id: number;
  anio: number;
  pagada: boolean;
  fechaPago?: string;
  alumnoId: number;
  valor: number;
}

// --- Métodos de Pago ---
export interface MetodoPagoRegistroRequest {
  descripcion: string;
}

export interface MetodoPagoModificacionRequest {
  descripcion: string;
  activo: boolean;
}

export interface MetodoPagoResponse {
  id: number;
  descripcion: string;
  activo: boolean;
}

// --- Valores para el formulario de cobranza ---
export interface CobranzasFormValues {
  reciboNro: string;
  alumno: string;
  inscripcionId: string;
  fecha: string;
  detalles: Array<{
    codigoConcepto?: string;
    concepto: string;
    cuota?: string;
    valorBase: number;
    bonificacion: number;
    recargo: number;
    aFavor: number;
    importe?: number;
    aCobrar?: number;
  }>;
  // Grupo para Disciplina y Tarifa
  disciplina: string;
  tarifa: string;
  claseSuelta?: number;
  clasePrueba?: number;
  // Grupo para Concepto y Stock
  conceptoSeleccionado: string;
  stockSeleccionado: string;
  cantidad: number;
  aFavor: number;
  totalCobrado: number;
  // Nuevo: campo para seleccionar un único método de pago
  metodoPagoId: string;
  observaciones: string;
  matriculaRemoved: boolean;
}

export interface DetalleCobranzaDTO {
  concepto: string;
  pendiente: number;
}

export interface CobranzaDTO {
  alumnoId: number;
  alumnoNombre: string;
  totalPendiente: number;
  detalles: DetalleCobranzaDTO[];
}
