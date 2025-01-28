// /src/types/types.ts

// ========================================================
// ALUMNO
// ========================================================

export interface AlumnoRequest {
  nombre: string;
  apellido: string;
  fechaNacimiento?: string;
  fechaIncorporacion?: string;
  celular1?: string;
  celular2?: string;
  email1?: string;
  email2?: string;
  documento?: string;
  cuit?: string;
  nombrePadres?: string;
  autorizadoParaSalirSolo?: boolean;
  activo?: boolean;
  otrasNotas?: string;
  cuotaTotal?: number;
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
}

// ========================================================
// DISCIPLINA
// ========================================================
export interface DisciplinaRequest {
  nombre: string;
  horario: string;
  frecuenciaSemanal: number;
  duracion: string;
  salon: string;
  valorCuota: number;
  matricula: number;
  profesorId: number;
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
  // por ejemplo, "inscritos": number si lo devuelves en el backend
}

// ========================================================
// BONIFICACION
// ========================================================
export interface BonificacionRequest {
  descripcion: string;
  porcentajeDescuento: number;
  activo?: boolean;
  observaciones?: string;
}

export interface BonificacionResponse {
  id: number;
  descripcion: string;
  porcentajeDescuento: number;
  activo: boolean;
  observaciones?: string;
}

// ========================================================
// INSCRIPCION
// ========================================================
export interface InscripcionRequest {
  alumnoId: number;
  disciplinaId: number;
  bonificacionId?: number;
  costoParticular?: number;
  notas?: string;
}

export interface InscripcionResponse {
  id: number;
  alumnoId: number;
  disciplinaId: number;
  bonificacionId?: number;
  costoParticular?: number;
  notas?: string;
}

// ========================================================
// OTROS (Profesor, Asistencia, etc.) según necesites
// ========================================================

// Profesor
export interface ProfesorRequest {
  nombre: string;
  apellido: string;
  especialidad?: string;
  aniosExperiencia?: number;
  usuarioId?: number;
}

export interface ProfesorResponse {
  id: number;
  nombre: string;
  apellido: string;
  especialidad?: string;
  aniosExperiencia?: number;
  usuarioId?: number | null;
}

// Asistencia
export interface AsistenciaRequest {
  fecha: string; // ISO Date
  presente: boolean;
  observacion?: string;
  alumnoId: number;
  disciplinaId: number;
}

export interface AsistenciaResponse {
  id: number;
  fecha: string;
  presente: boolean;
  observacion?: string;
  alumnoId: number;
  disciplinaId: number;
}
