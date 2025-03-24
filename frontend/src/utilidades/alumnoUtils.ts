import type {
  AlumnoResponse,
  AlumnoRegistroRequest,
} from "../types/types";

export const convertToAlumnoRegistroRequest = (
  alumno: AlumnoResponse
): AlumnoRegistroRequest => {
  console.log("Converting alumno:", alumno);
  return {
    nombre: alumno.nombre || "",
    apellido: alumno.apellido || "",
    fechaNacimiento: alumno.fechaNacimiento?.toString() || "",
    fechaIncorporacion: alumno.fechaIncorporacion?.toString() || "",
    celular1: alumno.celular1 || "",
    celular2: alumno.celular2 || "",
    email1: alumno.email1 || "",
    // email2 se elimina
    documento: alumno.documento || "",
    nombrePadres: alumno.nombrePadres || "",
    autorizadoParaSalirSolo: alumno.autorizadoParaSalirSolo || false,
    otrasNotas: alumno.otrasNotas || "",
    cuotaTotal: alumno.cuotaTotal || 0,
    inscripciones: [],
    edad: 0,
    activo: false,
  };
};
