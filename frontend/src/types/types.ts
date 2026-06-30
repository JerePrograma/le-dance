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

// types.ts
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface PageResponse<T> {
  observacion: string;
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ==========================================
// ALUMNO
// ==========================================
export interface AlumnoResponse {
  id: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string; // formato ISO (ej. "2023-08-30")
  fechaIncorporacion: string; // formato ISO
  edad: number;
  celular1: string;
  celular2: string;
  email: string;
  email2: string;
  documento: string;
  fechaDeBaja: string | null; // puede ser null si no se dio de baja
  nombrePadres: string;
  autorizadoParaSalirSolo: boolean;
  activo: boolean;
  otrasNotas: string;
  inscripciones: InscripcionResponse[]; // Asegúrate de que InscripcionResponse esté definido según tu DTO
}

export interface AlumnoRegistroRequest {
  id?: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string; // en formato ISO, ej. "2023-08-30"
  fechaIncorporacion: string; // en formato ISO
  edad: number;
  celular1?: string;
  celular2?: string;
  email?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  fechaDeBaja?: string | null;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  activo: boolean;
  otrasNotas?: string;
  inscripciones: InscripcionRegistroRequest[]; // Asegúrate de que InscripcionRegistroRequest esté definido según tu DTO
}

export interface AlumnoRegistro {
  id?: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string; // en formato ISO, ej. "2023-08-30"
  fechaIncorporacion: string; // en formato ISO
  edad: number;
  celular1?: string;
  celular2?: string;
  email?: string;
  email2?: string;
  documento?: string;
  fechaDeBaja?: string | null;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  activo: boolean;
  otrasNotas?: string;
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
export interface DisciplinaHorarioRequest {
  id?: number;
  diaSemana: DiaSemana;
  horarioInicio: LocalTime;
  duracion: number;
}

export interface DisciplinaRegistroRequest {
  id?: number;
  nombre: string;
  // Se elimina: diasSemana, horarioInicio y duracion
  frecuenciaSemanal?: number;
  salonId: number;
  profesorId: number;
  recargoId?: number;
  valorCuota: number;
  matricula: number;
  claseSuelta?: number;
  clasePrueba?: number;
  // Nuevo campo: lista de horarios para cada dia de clase
  horarios: DisciplinaHorarioRequest[];
}

export interface DisciplinaModificacionRequest {
  nombre: string;
  // Se elimina: diasSemana, horarioInicio y duracion
  frecuenciaSemanal?: number;
  salonId: number;
  profesorId: number;
  recargoId?: number;
  valorCuota: number;
  matricula: number;
  claseSuelta?: number;
  clasePrueba?: number;
  activo: boolean;
  // Nuevo campo: lista de horarios actualizados
  horarios: DisciplinaHorarioRequest[];
}

export interface DisciplinaResponse {
  salonId: number;
  recargoId: number | undefined;
  claseSuelta: number | undefined;
  clasePrueba: number | undefined;
  id: number;
  nombre: string;
  // Si se devuelve la lista de horarios, por ejemplo:
  horarios: DisciplinaHorarioResponse[];
  frecuenciaSemanal: number;
  salon: string;
  valorCuota: number;
  matricula: number;
  profesorId: number | null;
  inscritos: number;
  activo?: boolean;
}

export interface DisciplinaHorarioResponse {
  // Define aqui las propiedades que tenga un horario, por ejemplo:
  id?: number;
  diaSemana: string;
  horarioInicio: string;
  duracion: number;
}

export interface DisciplinaDetalleResponse {
  id: number;
  nombre: string;
  salon: string;
  salonId: number; // Agregado
  valorCuota: number;
  matricula: number;
  profesorNombre: string;
  profesorApellido: string;
  profesorId: number;
  inscritos: number;
  activo: boolean;
  claseSuelta?: number;
  clasePrueba?: number;
  recargoId?: number; // Puedes marcarlo como opcional si puede ser undefined
  horarios: DisciplinaHorarioResponse[]; // Agregado
}

export interface DisciplinaListadoResponse
  extends DisciplinaDetalleResponse {
  horarioInicio?: string;
}

// ==========================================
// ASISTENCIA
// ==========================================
// types/types.ts
export enum EstadoAsistencia {
  PRESENTE = "PRESENTE",
  AUSENTE = "AUSENTE",
}

// Para crear o actualizar una asistencia diaria (igual que antes)
export interface AsistenciaDiariaRegistroRequest {
  id?: number; // Opcional en creación
  fecha: string; // Formato ISO (YYYY-MM-DD)
  estado: EstadoAsistencia;
  asistenciaAlumnoMensualId: number;
  observacion?: string;
}

// Response de una asistencia diaria, ahora con un objeto "alumno"
export interface AsistenciaDiariaResponse {
  id: number;
  fecha: string;
  estado: EstadoAsistencia;
  asistenciaAlumnoMensualId: number;
  alumno: AlumnoResponse;
  asistenciaMensualId: number;
  disciplinaId: number;
  observacion?: string;
}

// Para crear una asistencia mensual (planilla)
export interface AsistenciaMensualRegistroRequest {
  mes: number;
  anio: number;
  disciplinaId: number;
  asistenciasDiarias?: AsistenciaDiariaRegistroRequest[];
}

// Para modificar la asistencia mensual (actualizando observaciones y, opcionalmente, asistencias diarias)
export interface AsistenciaMensualModificacionRequest {
  asistenciasAlumnoMensual: {
    id: number; // id del registro AsistenciaAlumnoMensual
    observacion: string;
    asistenciasDiarias?: AsistenciaDiariaRegistroRequest[];
  }[];
}

// Respuesta de cada registro de asistencia mensual de un alumno,
// ahora con el alumno anidado en lugar de campos sueltos
export interface AsistenciaAlumnoMensualDetalleResponse {
  id: number;
  asistenciaMensualId: number;
  inscripcionId: number;
  alumno: AlumnoResponse;
  observacion: string;
  asistenciasDiarias: AsistenciaDiariaResponse[];
}

// Representación de la disciplina, de forma anidada
export interface DisciplinaResponse {
  id: number;
  nombre: string;
}

// Respuesta detallada de la asistencia mensual (planilla)
// Incluye la disciplina como objeto anidado y el listado de registros de alumno
export interface AsistenciaMensualDetalleResponse {
  id: number;
  mes: number;
  anio: number;
  disciplina: DisciplinaResponse;
  profesor: string;
  alumnos: AsistenciaAlumnoMensualDetalleResponse[];
}

// Respuesta para listado de planillas mensuales
export interface AsistenciaMensualListadoResponse {
  id: number;
  mes: number;
  anio: number;
  disciplina: DisciplinaResponse;
  profesor: string;
  cantidadAlumnos: number;
}

export interface AsistenciasActivasResponse {
  totalInscripcionesProcesadas: number;
  totalPlanillasCreadas: number;
  totalAsistenciasDiariasGeneradas: number;
  detalles: string[];
}

// Para manejo de paginación (por ejemplo, en consultas de asistencias diarias)
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

// ==========================================
// INSCRIPCIÓN
// ==========================================

// Tipo para la solicitud de inscripción
export interface InscripcionRegistroRequest {
  // Si en creación el id no se envía, puede ser opcional o null
  id?: number | null;
  alumno: {
    id: number;
    nombre?: string;
    apellido?: string;
  };
  disciplina: DisciplinaRegistroRequest;
  bonificacionId?: number | null;
  // Se espera una cadena ISO (por ejemplo, "2025-03-10")
  fechaInscripcion: string;
  // La fecha de baja puede ser opcional
  fechaBaja?: string;
  // El costo particular es opcional (en caso de que se envíe)
  costoParticular?: number;
}
// Tipo para la respuesta de inscripción
export interface InscripcionResponse {
  id: number;
  alumno: AlumnoResponse;
  disciplina: DisciplinaListadoResponse;
  fechaInscripcion: string;
  estado: "ACTIVA" | "BAJA" | string;
  costoCalculado: number;
  bonificacion?: BonificacionResponse;
  mensualidadEstado: string;
}

export interface BonificacionRegistroRequest {
  descripcion: string;
  porcentajeDescuento?: number;
  observaciones?: string;
  valorFijo?: number;
}

export interface BonificacionModificacionRequest {
  descripcion: string;
  porcentajeDescuento?: number;
  activo: boolean;
  observaciones?: string;
  valorFijo?: number;
}

export interface BonificacionResponse {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
  valorFijo?: number;
}

// ==========================================
// RECARGOS - Tipos de datos
// ==========================================

/** Request para registrar un nuevo recargo */
export interface RecargoRegistroRequest {
  descripcion: string;
  porcentaje: number;
  valorFijo?: number;
  diaDelMesAplicacion: number; // ✅ Cambiado de fechaAplicacion a diaDelMesAplicacion
}

/** Request para actualizar un recargo */
export type RecargoModificacionRequest = Partial<RecargoRegistroRequest>;

/** Respuesta del backend al obtener un recargo */
export interface RecargoResponse {
  id: number;
  descripcion: string;
  porcentaje: number;
  valorFijo?: number;
  diaDelMesAplicacion: number; // ✅ Actualizado
}

// ==========================================
// STOCK
// ==========================================
export interface StockRegistroRequest {
  nombre: string;
  precio: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  // Nuevos atributos
  fechaIngreso: LocalDate; // Obligatorio en el registro
  fechaEgreso?: LocalDate; // Opcional
}

export interface StockModificacionRequest {
  nombre: string;
  precio: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  activo: boolean;
  // Nuevos atributos
  fechaIngreso: LocalDate;
  fechaEgreso?: LocalDate;
}

export interface StockResponse {
  version: number;
  id: number;
  nombre: string;
  precio: number;
  stock: number;
  requiereControlDeStock: boolean;
  codigoBarras?: string;
  activo: boolean;
  // Nuevos atributos en la respuesta:
  fechaIngreso: string;
  fechaEgreso?: string;
}

// ==========================================
// USUARIO Y ROLES
// ==========================================
// types.ts

export interface UsuarioRegistroRequest {
  nombreUsuario: string;
  contrasena: string;
  rol: string;
}

export interface UsuarioModificacionRequest {
  nombreUsuario: string;
  // La contraseña se hace opcional, para permitir actualizarla o dejarla sin cambios
  contrasena?: string;
  rol: string;
  activo: boolean;
}

export interface UsuarioResponse {
  id: number;
  nombreUsuario: string;
  rol: string;
  activo: boolean;
}

export interface RolRegistroRequest {
  descripcion: string;
}

export interface RolModificacionRequest {
  descripcion: string;
  activo: boolean;
}

export interface RolResponse {
  id: number;
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
  fechaGeneracion: string; // O LocalDate, segun prefieras
  usuarioId?: number;
  activo: boolean;
}

// src/types/conceptosTypes.ts
export interface ConceptoRegistroRequest {
  descripcion: string;
  precio: number;
  subConcepto: SubConceptoResponse;
  activo?: boolean;
}

export interface ConceptoResponse {
  version: number;
  id: number;
  descripcion: string;
  precio: number;
  subConcepto: SubConceptoResponse;
  activo?: boolean;
}

export interface SubConceptoRegistroRequest {
  descripcion: string;
}

export interface SubConceptoModificacionRequest {
  descripcion: string;
}

export interface SubConceptoResponse {
  id: number;
  descripcion: string;
}

// --- Metodos de Pago ---
// Interfaces actualizadas

export interface MetodoPagoRegistroRequest {
  descripcion: string;
  recargo: number;
}

export interface MetodoPagoModificacionRequest {
  descripcion: string;
  activo: boolean;
  recargo: number;
}

export interface MetodoPagoResponse {
  id: number;
  descripcion: string;
  activo: boolean;
  recargo: number;
}

export interface SalonRegistroRequest {
  nombre: string;
  descripcion: string;
}

export interface SalonModificacionRequest {
  nombre: string;
  descripcion: string;
}

export interface SalonResponse {
  id: number;
  nombre: string;
  descripcion: string;
}

export interface PagoRegistroRequest {
  alumnoId: number;
  metodoPagoId: number;
  montoRecibido: string;
  idempotencyKey: string;
  observaciones?: string;
  aplicaciones: AplicacionPagoRequest[];
  generarCredito: boolean;
}

export interface AplicacionPagoRequest {
  cargoId: number;
  importe: string;
}

export interface AplicacionPagoResponse {
  id: number;
  cargoId: number;
  importeAplicado: string;
  estado: string;
  saldoCargo: string;
}

export interface PagoResponse {
  id: number;
  alumnoId: number;
  metodoPagoId: number;
  usuarioId: number;
  fecha: string;
  montoRecibido: string;
  estado: string;
  idempotencyKey: string;
  observaciones?: string;
  creditoGenerado: string;
  aplicaciones: AplicacionPagoResponse[];
}

export interface PagoAnulacionRequest {
  idempotencyKey: string;
  motivo: string;
}

export interface CargoResponse {
  id: number;
  alumnoId: number;
  tipo: string;
  descripcion: string;
  importeOriginal: string;
  importeAplicado: string;
  saldo: string;
  fechaEmision: string;
  fechaVencimiento: string;
  estado: string;
}

export interface MensualidadRegistroRequest {
  inscripcionId: number;
  anio: number;
  mes: number;
}

export interface MensualidadResponse {
  id: number;
  inscripcionId: number;
  anio: number;
  mes: number;
  descripcion: string;
  estado: string;
  cargoId: number;
  importe: string;
}
export interface MatriculaRegistroRequest {
  alumnoId: number;
  anio: number;
}

export interface MatriculaResponse {
  id: number;
  anio: number;
  fechaEmision: string;
  estado: string;
  alumnoId: number;
}

export interface EgresoRegistroRequest {
  fecha?: string;
  monto: string;
  observaciones?: string;
  metodoPagoId: number;
  idempotencyKey: string;
}

export interface EgresoResponse {
  id: number;
  fecha: string;
  monto: string;
  observaciones?: string;
  metodoPagoId: number;
  usuarioId: number;
  estado: string;
  idempotencyKey: string;
}

export interface MovimientoCajaResponse {
  id: number;
  tipo: string;
  fecha: string;
  importe: string;
  metodoPagoId?: number;
  pagoId?: number;
  egresoId?: number;
  motivo?: string;
  createdAt: string;
}

export interface ResumenCajaResponse {
  desde: string;
  hasta: string;
  totalIngresos: string;
  totalEgresos: string;
  saldo: string;
  movimientos: MovimientoCajaResponse[];
}

export type ObservacionProfesorResponse = {
  id: number;
  profesorId: number;
  fecha: string; // ISO date string
  observacion: string;
};

export type ObservacionProfesorRequest = {
  profesorId: number;
  fecha: string; // ISO date string
  observacion: string;
};
