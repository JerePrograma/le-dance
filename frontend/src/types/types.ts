// ==========================================
// Utility Types & Enums
// ==========================================
type LocalDate = string;
type LocalTime = string;

export enum EstadoAsistencia {
  Presente = "Presente",
  Ausente = "Ausente",
}

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

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

// ==========================================
// Alumno
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

// ==========================================
// Profesor
// ==========================================
export interface ProfesorRegistroRequest {
  nombre: string;
  apellido: string;
  especialidad?: string;
  fechaNacimiento?: LocalDate;
  telefono?: string;
}

export interface ProfesorModificacionRequest {
  nombre: string;
  apellido: string;
  especialidad?: string;
  fechaNacimiento?: LocalDate;
  telefono?: string;
  activo: boolean;
}

export interface ProfesorDetalleResponse {
  id: number;
  nombre: string;
  apellido: string;
  especialidad?: string;
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
// Disciplina
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
// Asistencia
// ==========================================
export type AsistenciaMensualRegistroRequest = {
  mes: number;
  anio: number;
  inscripcionId?: number; // Hacerlo opcional si puede no tener alumnos
  asistenciasDiarias?: AsistenciaDiariaRegistroRequest[];
};

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

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ==========================================
// Inscripción
// ==========================================
export interface InscripcionDisciplinaRequest {
  disciplinaId: number;
  bonificacionId?: number;
}

export interface InscripcionRegistroRequest {
  alumnoId: number;
  inscripcion: InscripcionDisciplinaRequest;
  fechaInscripcion?: string; // 👈 Hacerla opcional
  notas?: string;
}

export interface InscripcionModificacionRequest {
  alumnoId: number;
  disciplinaId: number;
  bonificacionId?: number;
  fechaBaja?: string; // Changed from LocalDate to string
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
// Bonificación
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
// Pagos y Métodos de Pago
// ==========================================
export interface PagoRegistroRequest {
  fecha: LocalDate;
  fechaVencimiento: LocalDate;
  monto: number;
  inscripcionId: number;
  metodoPagoId?: number;
  recargoAplicado?: boolean;
  bonificacionAplicada?: boolean;
  saldoRestante: number;
}

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

// ==========================================
// Caja
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

// ==========================================
// Recargos
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
// Salón
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
// Productos
// ==========================================
export interface ProductoRegistroRequest {
  nombre: string;
  precio: number;
  tipoProductoId?: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
}

export interface ProductoModificacionRequest {
  nombre: string;
  precio: number;
  tipoProductoId?: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  activo: boolean;
}

export interface TipoProductoRegistroRequest {
  descripcion: string;
}

export interface TipoProductoModificacionRequest {
  descripcion: string;
  activo: boolean;
}

// ==========================================
// Usuario y Roles
// ==========================================
export interface UsuarioRegistroRequest {
  nombreUsuario: string;
  email: string;
  contrasena: string;
  rol: string;
}

export interface UsuarioModificacionRequest {
  nombreUsuario: string;
  email: string;
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
// Reportes
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
