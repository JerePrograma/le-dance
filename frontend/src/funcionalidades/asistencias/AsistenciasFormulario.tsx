import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Field, Form, Formik, ErrorMessage } from "formik"; // ✅ Mantuve ErrorMessage por si decides usarlo
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import asistenciasApi from "../../utilidades/asistenciasApi";
import { toast } from "react-toastify";
import type {
  AsistenciaRequest,
  AlumnoListadoResponse,
  DisciplinaResponse,
  ProfesorListadoResponse,
} from "../../types/types";
import Boton from "../../componentes/comunes/Boton";

const initialAsistenciaValues: AsistenciaRequest = {
  id: 0,
  fecha: "",
  alumnoId: 0,
  disciplinaId: 0,
  profesorId: 0,
  presente: true,
  observacion: "",
};

const AsistenciasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [asistenciaId, setAsistenciaId] = useState<number | null>(null);
  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas] = useState<DisciplinaResponse[]>([]);
  const [profesores, setProfesores] = useState<ProfesorListadoResponse[]>([]);
  const [mensaje, setMensaje] = useState(""); // ✅ Mantuve mensaje por si quieres usarlo

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id, () => {});
    }
  }, [searchParams]);

  const handleBuscar = async (
    idStr: string,
    callback: (vals: AsistenciaRequest) => void
  ) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID inválido");
        return;
      }

      const asistencia = await asistenciasApi.obtenerAsistenciaPorId(idNum);

      const asistenciaFormateada: AsistenciaRequest = {
        id: asistencia.id,
        fecha: asistencia.fecha,
        presente: asistencia.presente,
        observacion: asistencia.observacion ?? "",
        alumnoId: asistencia.alumno.id,
        disciplinaId: asistencia.disciplina.id,
        profesorId: asistencia.profesor?.id ?? 0,
      };

      callback(asistenciaFormateada);
      setAsistenciaId(asistencia.id);
      setMensaje("");
    } catch {
      setMensaje("Asistencia no encontrada.");
      callback(initialAsistenciaValues);
      setAsistenciaId(null);
    }
  };

  const handleGuardarAsistencia = async (values: AsistenciaRequest) => {
    try {
      if (asistenciaId) {
        await asistenciasApi.actualizarAsistencia(asistenciaId, values);
        setMensaje("Asistencia actualizada correctamente.");
      } else {
        const nuevaAsistencia = await asistenciasApi.registrarAsistencia(
          values
        );
        setAsistenciaId(nuevaAsistencia.id);
        setMensaje("Asistencia registrada correctamente.");
      }
    } catch {
      setMensaje("Error al guardar la asistencia.");
    }
  };

  const handleSeleccionarDisciplina = async (
    id: number,
    setFieldValue: (field: string, value: any) => void
  ) => {
    if (!id) return;

    try {
      const [alumnosData, profesoresData] = await Promise.all([
        asistenciasApi.obtenerAlumnosDeDisciplina(id),
        asistenciasApi.obtenerProfesoresDeDisciplina(id),
      ]);

      setAlumnos(alumnosData);
      setProfesores(profesoresData);

      setFieldValue("disciplinaId", id);
      setFieldValue("alumnoId", 0);
      setFieldValue("profesorId", 0);
    } catch {
      toast.error("Error al cargar datos de la disciplina.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      {mensaje && <p className="form-mensaje">{mensaje}</p>}{" "}
      {/* ✅ Ahora mensaje se usa */}
      <Formik
        initialValues={initialAsistenciaValues}
        validationSchema={asistenciaEsquema}
        onSubmit={handleGuardarAsistencia}
        enableReinitialize
      >
        {({ resetForm, isSubmitting, setFieldValue }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* 🔹 Selección de Disciplina */}
              <label htmlFor="disciplinaId" className="auth-label">
                Disciplina:
              </label>
              <Field
                as="select"
                id="disciplinaId"
                name="disciplinaId"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                  handleSeleccionarDisciplina(
                    Number(e.target.value),
                    setFieldValue
                  )
                }
              >
                <option value="">Seleccione una disciplina</option>
                {disciplinas.map((disc) => (
                  <option key={disc.id} value={disc.id}>
                    {disc.nombre}
                  </option>
                ))}
              </Field>

              {/* 🔹 Selección de Alumno (Filtrado por Disciplina) */}
              <label htmlFor="alumnoId" className="auth-label">
                Alumno:
              </label>
              <Field
                as="select"
                id="alumnoId"
                name="alumnoId"
                className="form-input"
              >
                <option value="">Seleccione un alumno</option>
                {alumnos.map((alumno) => (
                  <option key={alumno.id} value={alumno.id}>
                    {alumno.nombre} {alumno.apellido}
                  </option>
                ))}
              </Field>

              {/* 🔹 Selección de Profesor (Filtrado por Disciplina) */}
              <label htmlFor="profesorId" className="auth-label">
                Profesor:
              </label>
              <Field
                as="select"
                id="profesorId"
                name="profesorId"
                className="form-input"
              >
                <option value="">Seleccione un profesor</option>
                {profesores.map((prof) => (
                  <option key={prof.id} value={prof.id}>
                    {prof.nombre} {prof.apellido}
                  </option>
                ))}
              </Field>

              <ErrorMessage
                name="disciplinaId"
                component="div"
                className="auth-error"
              />
            </div>

            <div className="form-acciones">
              <Boton
                type="submit"
                disabled={isSubmitting}
                className="page-button"
              >
                Guardar Asistencia
              </Boton>
              <Boton
                type="reset"
                onClick={() => resetForm()}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                onClick={() => navigate("/asistencias")}
                className="page-button-secondary"
              >
                Volver al Listado
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AsistenciasFormulario;
