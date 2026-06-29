import type {
    AlumnoRegistroRequest,
    AlumnoResponse,
    InscripcionRegistroRequest,
    InscripcionResponse,
} from "../../types/types";

// Función para normalizar una inscripción (InscripcionResponse → InscripcionRegistroRequest)
export const normalizeInscripcion = (insc: InscripcionResponse): InscripcionRegistroRequest => ({
    id: insc.id,
    disciplina: {
        id: insc.disciplina.id,
        nombre: insc.disciplina.nombre,
        frecuenciaSemanal: 0,
        salonId: 0,
        profesorId: 0,
        recargoId: 0,
        valorCuota: insc.disciplina.valorCuota,
        matricula: 0,
        claseSuelta: insc.disciplina.claseSuelta,
        clasePrueba: insc.disciplina.clasePrueba,
        horarios: [],
    },
    bonificacionId: insc.bonificacion ? insc.bonificacion.id : undefined,
    fechaInscripcion: insc.fechaInscripcion,
    alumno: { id: insc.alumno.id, nombre: insc.alumno.nombre, apellido: insc.alumno.apellido }
});

export const normalizeAlumno = (alumno: AlumnoResponse): AlumnoRegistroRequest => ({
    id: alumno.id,
    nombre: alumno.nombre,
    apellido: alumno.apellido,
    fechaNacimiento: alumno.fechaNacimiento,
    fechaIncorporacion: alumno.fechaIncorporacion,
    edad: alumno.edad,
    celular1: alumno.celular1,
    celular2: alumno.celular2,
    email: alumno.email,
    email2: alumno.email2,
    documento: alumno.documento,
    fechaDeBaja: alumno.fechaDeBaja,
    deudaPendiente: alumno.deudaPendiente,
    nombrePadres: alumno.nombrePadres,
    autorizadoParaSalirSolo: alumno.autorizadoParaSalirSolo,
    activo: alumno.activo,
    otrasNotas: alumno.otrasNotas,
    cuotaTotal: alumno.cuotaTotal,
    creditoAcumulado: alumno.creditoAcumulado,
    inscripciones: alumno.inscripciones.map(normalizeInscripcion),
});
