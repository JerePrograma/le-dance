import { InscripcionRegistroRequest, InscripcionResponse } from "../../types/types";

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
