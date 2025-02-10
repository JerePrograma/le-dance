import type {
  AlumnoDetalleResponse,
  AlumnoRegistroRequest,
  AlumnoModificacionRequest,
  InscripcionDisciplinaRequest,
} from "../types/types";

export const convertToAlumnoRegistroRequest = (
  alumno: AlumnoDetalleResponse
): AlumnoRegistroRequest => {
  console.log("Converting alumno:", alumno);
  return {
    nombre: alumno.nombre || "",
    apellido: alumno.apellido || "",
    fechaNacimiento: alumno.fechaNacimiento || "",
    fechaIncorporacion: alumno.fechaIncorporacion || "",
    celular1: alumno.celular1 || "",
    celular2: alumno.celular2 || "",
    email1: alumno.email1 || "",
    email2: alumno.email2 || "",
    documento: alumno.documento || "",
    cuit: alumno.cuit || "",
    nombrePadres: alumno.nombrePadres || "",
    autorizadoParaSalirSolo: alumno.autorizadoParaSalirSolo || false,
    otrasNotas: alumno.otrasNotas || "",
    cuotaTotal: alumno.cuotaTotal || 0,
    disciplinas: [], // Assuming this should be empty when converting from detail to registration
  };
};

export const convertToAlumnoModificacionRequest = (
  values: AlumnoModificacionRequest
): AlumnoModificacionRequest => {
  return {
    nombre: values.nombre,
    apellido: values.apellido,
    fechaNacimiento: values.fechaNacimiento,
    celular1: values.celular1,
    celular2: values.celular2,
    email1: values.email1,
    email2: values.email2,
    documento: values.documento,
    cuit: values.cuit,
    nombrePadres: values.nombrePadres,
    autorizadoParaSalirSolo: values.autorizadoParaSalirSolo,
    otrasNotas: values.otrasNotas,
    cuotaTotal: values.cuotaTotal,
    activo: values.activo,
    disciplinas: values.disciplinas || [],
  };
};

export const createInscripcionDisciplinaRequest = (
  disciplinaId: number,
  bonificacionId?: number
): InscripcionDisciplinaRequest => {
  return {
    disciplinaId,
    bonificacionId,
  };
};
