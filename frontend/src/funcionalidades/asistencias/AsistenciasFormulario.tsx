import { useEffect, useState, useCallback } from "react";
import { Form, useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Formik } from "formik";
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import asistenciasApi from "../../utilidades/asistenciasApi";
import { toast } from "react-toastify";
import type {
  AsistenciaRequest,
  AlumnoListadoResponse,
  DisciplinaResponse,
  ProfesorResponse,
} from "../../types/types";
import Boton from "../../componentes/comunes/Boton";
import { Search } from "lucide-react";

const initialAsistenciaValues: AsistenciaRequest = {
  id: 0,
  fecha: "",
  alumnoId: 0,
  disciplinaId: 0,
  presente: true,
  observacion: "",
};

const AsistenciasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [profesores, setProfesores] = useState<ProfesorResponse[]>([]);
  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [alumnosFiltrados, setAlumnosFiltrados] = useState<
    AlumnoListadoResponse[]
  >([]);
  const [profesoresFiltrados, setProfesoresFiltrados] = useState<
    ProfesorResponse[]
  >([]);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [profesoresData, alumnosData, disciplinasData] = await Promise.all([
        asistenciasApi.obtenerProfesores(),
        asistenciasApi.obtenerAlumnosListado(),
        asistenciasApi.obtenerDisciplinas(),
      ]);
      setProfesores(profesoresData);
      setAlumnos(alumnosData);
      setAlumnosFiltrados(alumnosData);
      setDisciplinas(disciplinasData);
    } catch {
      toast.error("Error al cargar datos iniciales.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id, () => {});
    }
  }, [searchParams]);

  const handleBuscar = async (
    idStr: string,
    resetForm: (nextState?: { values: AsistenciaRequest }) => void
  ) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID inválido");
        return;
      }
      const data = await asistenciasApi.obtenerAsistenciaPorId(idNum);
      toast.success("Asistencia cargada correctamente.");
      resetForm({ values: data });
    } catch {
      toast.error("Error al cargar la asistencia.");
      resetForm({ values: initialAsistenciaValues });
    }
  };

  const handleGuardarAsistencia = async (values: AsistenciaRequest) => {
    try {
      if (values.id) {
        await asistenciasApi.actualizarAsistencia(values.id, values);
      } else {
        await asistenciasApi.registrarAsistencia(values);
      }
      toast.success("Asistencia guardada correctamente.");
      navigate("/asistencias");
    } catch {
      toast.error("Error al guardar la asistencia.");
    }
  };

  const handleSeleccionarDisciplina = (
    disciplinaId: number,
    values: AsistenciaRequest,
    setValues: (values: AsistenciaRequest) => void
  ) => {
    setValues({ ...values, disciplinaId, profesorId: 0 });
    const profesoresRelacionados = profesores.filter(
      (prof) =>
        prof.id === disciplinas.find((d) => d.id === disciplinaId)?.profesorId
    );
    setProfesoresFiltrados(profesoresRelacionados);
  };

  const handleFiltrarAlumnos = async (fecha: string, disciplinaId: number) => {
    try {
      const response = await asistenciasApi.obtenerAlumnosPorFechaYDisciplina(
        fecha,
        disciplinaId
      );
      setAlumnosFiltrados(response);
    } catch {
      toast.error("Error al cargar alumnos para la disciplina seleccionada.");
      setAlumnosFiltrados([]);
    }
  };

  if (loading) return <div className="text-center py-4">Cargando datos...</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      <Formik
        initialValues={initialAsistenciaValues}
        validationSchema={asistenciaEsquema}
        onSubmit={handleGuardarAsistencia}
        enableReinitialize
      >
        {({ resetForm, setValues, isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Asistencia:
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    id="idBusqueda"
                    className="form-input flex-grow"
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleBuscar(e.target.value, resetForm)
                    }
                  />
                  <Boton
                    onClick={() =>
                      handleBuscar(values.id?.toString() || "", resetForm)
                    }
                    className="page-button"
                  >
                    <Search className="w-5 h-5 mr-2" /> Buscar
                  </Boton>
                </div>
              </div>

              <Field
                type="date"
                id="fecha"
                name="fecha"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  setValues({
                    ...values,
                    fecha: e.target.value,
                    disciplinaId: 0,
                    alumnoId: 0,
                  });
                  setAlumnosFiltrados(alumnos);
                }}
              />
              <ErrorMessage
                name="fecha"
                component="div"
                className="auth-error"
              />

              <Field
                as="select"
                id="disciplinaId"
                name="disciplinaId"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                  const id = Number(e.target.value);
                  handleSeleccionarDisciplina(id, values, setValues);
                  handleFiltrarAlumnos(values.fecha, id);
                }}
              >
                <option value="">Seleccione una disciplina</option>
                {disciplinas.map((disc) => (
                  <option key={disc.id} value={disc.id}>
                    {disc.nombre}
                  </option>
                ))}
              </Field>
              <Field
                as="select"
                id="profesorId"
                name="profesorId"
                className="form-input"
              >
                <option value="">Seleccione un profesor</option>
                {profesoresFiltrados.map((prof) => (
                  <option key={prof.id} value={prof.id}>
                    {prof.nombre} {prof.apellido}
                  </option>
                ))}
              </Field>

              <Field
                as="select"
                id="alumnoId"
                name="alumnoId"
                className="form-input"
              >
                <option value="">Seleccione un alumno</option>
                {alumnosFiltrados.map((alumno) => (
                  <option key={alumno.id} value={alumno.id}>
                    {alumno.nombre} {alumno.apellido}
                  </option>
                ))}
              </Field>
              <ErrorMessage
                name="disciplinaId"
                component="div"
                className="auth-error"
              />

              <Field
                as="textarea"
                id="observacion"
                name="observacion"
                className="form-input h-24"
              />

              <Field
                type="checkbox"
                name="presente"
                className="form-checkbox"
              />

              <div className="form-acciones">
                <Boton
                  type="submit"
                  disabled={isSubmitting}
                  className="page-button"
                >
                  Guardar
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
                  Volver
                </Boton>
              </div>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AsistenciasFormulario;
