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
export interface AlumnoRegistroRequest {
  id?: number;
  nombre: string;
  apellido: string;
  fechaNacimiento: string;      // Formato ISO (ej. "2025-03-10")
  fechaIncorporacion: string;     // Formato ISO
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
  // Se renombra el campo de 'disciplinas' a 'inscripciones' para ser coherente con el backend
  inscripciones: InscripcionRegistroRequest[];
}

export interface AlumnoModificacionRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento: string;
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
  // Ahora se utiliza 'inscripciones' (tipo InscripcionRegistroRequest) en lugar de 'disciplinas'
  inscripciones: InscripcionRegistroRequest[];
}

export interface AlumnoDetalleResponse {
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

// Representación del alumno (extraído de la inscripción)
export interface AlumnoResponse {
  id: number;
  nombre: string;
  apellido: string;
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

// Para manejo de paginación (por ejemplo, en consultas de asistencias diarias)
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

// Resumen simplificado de una disciplina (para listados)
export interface DisciplinaListadoResponse {
  id: number;
  nombre: string;
  // Otros campos opcionales
}

// ==========================================
// INSCRIPCIÓN
// ==========================================

export interface InscripcionRegistroRequest {
  id?: number; // opcional en creación
  alumnoId: number;
  disciplina: DisciplinaRegistroRequest;
  bonificacionId?: number;
  fechaInscripcion?: string; // ISO string, ej. "2025-03-10"
}

export interface InscripcionModificacionRequest {
  alumnoId: number;
  // Se utiliza el objeto de disciplina completo
  disciplina: DisciplinaRegistroRequest;
  bonificacionId?: number;
  fechaBaja?: string; // string ISO, opcional
  costoParticular?: number;
  activo?: boolean;
  estado?: string; // Puedes definir un enum o union de string (por ejemplo: "ACTIVA" | "BAJA")
}

// ==========================================
// BONIFICACIÓN
// ==========================================
// inscripcionTypes.ts

export interface InscripcionRequest {
  alumnoId: number;
  disciplinaId: number;
  bonificacionId?: number;
  fechaInscripcion: string;
  fechaBaja?: string;         // Opcional, para actualización
  costoParticular?: number;   // Opcional, para actualización
}

// Para el formulario, agregamos opcionalmente el id (para edición)
export interface InscripcionFormData extends InscripcionRequest {
  id?: number;
}

// La respuesta se mantiene igual (o se ajusta según convenga)

export interface InscripcionResponse {
  id: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
    // otros campos relevantes
  };
  disciplina: {
    id: number;
    nombre: string;
    valorCuota: number;
    claseSuelta: number;
    clasePrueba: number;
    // otros campos si es necesario
  };
  fechaInscripcion: string;
  estado: string; // por ejemplo "ACTIVA" o "BAJA"
  costoCalculado: number;
  bonificacion?: {
    id: number;
    descripcion: string;
    valorFijo: number;
    porcentajeDescuento: number;
  };
  mensualidadEstado: string; // Por ejemplo "PAGADO" o "PENDIENTE"
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
// CAJA
// ==========================================
export interface CajaRegistroRequest {
  fecha: LocalDate;
  totalEfectivo: number;
  totalDebito: number;

  rangoDesdeHasta?: string;
  observaciones?: string;
}

export interface CajaModificacionRequest {
  fecha: LocalDate;
  totalEfectivo: number;
  totalDebito: number;

  rangoDesdeHasta?: string;
  observaciones?: string;
  activo: boolean;
}

export interface CajaResponse {
  id: number;
  fecha: LocalDate;
  totalEfectivo: number;
  totalDebito: number;

  rangoDesdeHasta: string;
  observaciones: string;
  activo: boolean;
}

// ==========================================
// RECARGOS - Tipos de datos
// ==========================================

/** Request para registrar un nuevo recargo */
export interface RecargoRegistroRequest {
  descripcion: string;
  porcentaje: number;
  valorFijo?: number; // ✅ Opcional porque el backend lo permite
  diaDelMesAplicacion: number; // ✅ Cambiado de fechaAplicacion a diaDelMesAplicacion
}

/** Request para actualizar un recargo */
export interface RecargoModificacionRequest
  extends Partial<RecargoRegistroRequest> { }

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

// DTO para reporte de mensualidades
export interface ReporteMensualidadDTO {
  mensualidadId: number;
  alumno: {
    id: number;
    nombre: string;
  };
  cuota: string;
  importe: number;
  bonificacion: {
    id: number;
    descripcion: string;
    porcentajeDescuento: number;
    valorFijo: number;
  };
  total: number;
  recargo: number;
  estado: string;
  disciplina: {
    id: number;
    nombre: string;
    valorCuota: number;
  };
  descripcion: string; // Nuevo campo agregado
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

///CAJAS



export interface AlumnoMin {
  id: number;
  nombre: string;
  apellido: string;
}

export interface Pago {
  descripcion: any;
  id: number;
  fecha: string; // "2025-02-26"
  monto: number;
  observaciones?: string;
  alumno?: AlumnoMin; // { id, nombre, apellido }
  // metodo de pago, etc., segun tu back
}

export interface Egreso {
  id: number;
  fecha: string;
  monto: number;
  observaciones?: string;
  // etc...
}

export interface CajaDiariaDTO {
  fecha: string; // "2025-02-26"
  rangoRecibos: string; // "Recibo #10 al #12"
  totalEfectivo: number;
  totalDebito: number;
  totalEgresos: number;
  totalNeto: number;
}

export interface CajaDetalleDTO {
  pagosDelDia: Pago[];
  egresosDelDia: Egreso[];
}

export interface RendicionDTO {
  pagos: Pago[];
  egresos: Egreso[];
  totalEfectivo: number;
  totalDebito: number;
  totalEgresos: number;
}

// ==========================================
// PAGO Y MÉTODOS DE PAGO
// ==========================================

// PETICIÓN DE MODIFICACIÓN DE PAGO (para actualizar en el backend)
export interface PagoModificacionRequest {
  fecha: string;
  fechaVencimiento: string;
  metodoPagoId?: number;
  recargoAplicado?: boolean;
  bonificacionAplicada?: boolean;
  activo: boolean;
  detallePagos: {
    codigoConcepto?: string;
    concepto: string;
    cuota?: string;
    valorBase: number;
    bonificacionId?: number;
    recargoId?: number;
    // Se eliminan "aFavor" e "importe" porque se calculan internamente.
    aCobrar: number;
  }[];
  pagoMedios: PagoMedioRegistroRequest[];
}

// RESPUESTA DE CADA DETALLE DEL PAGO
export interface DetallePagoResponse {
  id: number;
  codigoConcepto?: string;
  concepto: string;
  cuota?: string;
  valorBase: number;
  bonificacion?: {
    id: number;
    descripcion: string;
    porcentajeDescuento: number;
    valorFijo?: number;
  };
  recargo?: {
    id: number;
    descripcion: string;
  };
  aFavor: number;
  aCobrar: number;
  importe: number;
  cobrado?: boolean;
}

export interface PagoRegistroRequest {
  alumno: {
    id: number;
    nombre: string;
  };
  fecha: string;
  fechaVencimiento: string;
  monto: number;
  // Ahora se envía la inscripción completa
  inscripcion: InscripcionRegistroRequest;
  metodoPagoId?: number;
  recargoAplicado?: boolean;
  bonificacionAplicada?: boolean;
  pagoMatricula: boolean;
  activo: boolean;
  detallePagos: Array<{
    codigoConcepto?: string;
    concepto: string;
    cuota?: string;
    valorBase: number;
    bonificacionId?: number;
    recargoId?: number;
    aCobrar: number;
  }>;
  pagoMedios?: PagoMedioRegistroRequest[];
}

// PETICIÓN DE REGISTRO DE PAGO PARA DETALLE (para el registro)
export interface DetallePagoRegistroRequest {
  codigoConcepto?: string;
  concepto: string;
  cuota?: string;
  valorBase: number;
  bonificacionId?: number;
  recargoId?: number;
  aCobrar: number;
  cobrado: boolean;
}

// PETICIÓN DE REGISTRO DE PAGO MEDIO (para abonos parciales)
export interface PagoMedioRegistroRequest {
  monto: number;
  metodoPagoId: number;
}

export interface PagoMedioResponse {
  id: number;
  monto: number;
  metodo: MetodoPagoResponse; // Asegúrate de que MetodoPagoResponse esté definido (al menos con id y descripción)
}

// RESPUESTA DEL PAGO (para recepción del backend)
export interface PagoResponse {
  id: number;
  fecha: string;
  fechaVencimiento: string;
  monto: number;
  metodoPago: number;       // Descripción del método de pago
  recargoAplicado: boolean;
  bonificacionAplicada: boolean;
  saldoRestante: number;
  saldoAFavor: number;
  activo: boolean;
  estadoPago: string;       // "ACTIVO" o "HISTÓRICO"
  inscripcion: InscripcionResponse;
  alumnoId: number;         // Derivado de la inscripción o enviado directamente en pagos generales
  observaciones: string;
  detallePagos: DetallePagoResponse[];
  pagoMedios: PagoMedioResponse[];
  /**
   * Indica el origen del pago: "SUBSCRIPTION" para pagos con inscripción, o "GENERAL" para pagos sin inscripción.
   */
  tipoPago: string;
}

// --- Valores para el formulario de cobranza ---
// Estos son los valores que usará el formulario para la UI.
export interface CobranzasFormValues {
  id: number;
  totalACobrar: number;
  reciboNro: string;
  alumno: string;
  inscripcion: InscripcionRegistroRequest;
  inscripcionId?: number;
  alumnoId?: string;
  fecha: string;
  detallePagos: Array<{
    id?: number | null;
    codigoConcepto?: number | string;
    concepto: string;
    cuota?: string;
    valorBase: number;
    bonificacionId?: string;
    recargoId?: string;
    aFavor: number;
    importe: number;
    aCobrar: number;
    autoGenerated?: boolean;
    abono?: number;
    _tempId?: number; // <-- Agregamos esta propiedad
  }>;
  mensualidadId?: string;
  disciplina: string;
  tarifa: string;
  claseSuelta?: number;
  clasePrueba?: number;
  conceptoSeleccionado: string;
  stockSeleccionado: string;
  cantidad: number;
  totalCobrado: number;
  metodoPagoId: number;
  observaciones: string;
  matriculaRemoved: boolean;
  periodoMensual: string;
  autoRemoved: number[];
  // Agregar el campo para el monto del abono parcial general:
  pagoParcial: number;
}

// --- Para la cobranza ---
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


export interface MensualidadRegistroRequest {
  fechaCuota: LocalDate;
  valorBase: number;
  recargo: number;
  bonificacion: number;
  inscripcionId: number;
}

export interface MensualidadModificacionRequest {
  fechaCuota: LocalDate;
  valorBase: number;
  recargo: number;
  bonificacion: number;
  estado: string; // Ej.: "PENDIENTE", "PAGADO", "OMITIDO"
}

export interface MensualidadResponse {
  descripcion: string;
  id: number;
  fechaCuota: LocalDate;  // Asegúrate de que LocalDate esté definido, o utiliza string en formato ISO
  valorBase: number;
  recargoId: number;
  bonificacionId: number;
  estado: string;         // Ej.: "PENDIENTE", "PAGADO", "OMITIDO"
  inscripcionId: number;
  totalPagar: number;     // Ahora es number en lugar de any
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

export interface DeudasPendientesResponse {
  alumnoId: number;
  alumnoNombre: string;
  pagosPendientes: PagoResponse[];
  matriculaPendiente: MatriculaResponse | null;
  mensualidadesPendientes: MensualidadResponse[];
  totalDeuda: number;
}

export interface PagoParcialRequest {
  montoAbonado: number;
  montosPorDetalle: { [detalleId: number]: number };
  metodoPagoId: number;
}
