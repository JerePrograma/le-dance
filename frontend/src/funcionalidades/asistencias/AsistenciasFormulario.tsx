import type React from "react";
import { useEffect, useState, useCallback } from "react";
import { Form, useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Formik } from "formik";
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import type {
  AsistenciaRequest,
  AsistenciaResponse,
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

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [profesoresResponse, alumnosResponse, disciplinasResponse] =
          await Promise.all([
            api.get<ProfesorResponse[]>("/api/profesores"),
            api.get<AlumnoListadoResponse[]>("/api/alumnos/listado"),
            api.get<DisciplinaResponse[]>("/api/disciplinas"),
          ]);
        setProfesores(profesoresResponse.data);
        setAlumnos(alumnosResponse.data);
        setAlumnosFiltrados(alumnosResponse.data);
        setDisciplinas(disciplinasResponse.data);
      } catch {
        toast.error("Error al cargar datos iniciales.");
      }
    };
    fetchData();
  }, []);

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) handleBuscar(id, () => {});
  }, [searchParams]);

  const handleBuscar = useCallback(
    async (idStr: string, resetForm: (values: AsistenciaRequest) => void) => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID inválido");
          return;
        }
        const data: AsistenciaResponse = await api.get(
          `/api/asistencias/${idNum}`
        );
        resetForm({
          id: data.id,
          fecha: data.fecha,
          alumnoId: data.alumnoId,
          disciplinaId: data.disciplinaId,
          presente: data.presente,
          observacion: data.observacion || "",
        });
        toast.success("Asistencia cargada correctamente.");
      } catch {
        toast.error("Error al cargar la asistencia.");
        resetForm(initialAsistenciaValues);
      }
    },
    []
  );

  const handleGuardarAsistencia = useCallback(
    async (values: AsistenciaRequest) => {
      try {
        await api.post("/api/asistencias", values);
        toast.success("Asistencia registrada correctamente.");
      } catch {
        toast.error("Error al registrar la asistencia.");
      }
    },
    []
  );

  const handleSeleccionarDisciplina = (
    disciplinaId: number,
    values: AsistenciaRequest,
    setValues: (values: AsistenciaRequest) => void
  ) => {
    setValues({
      ...values,
      disciplinaId,
      profesorId: 0,
    });

    const profesoresRelacionados = profesores.filter(
      (profesor) =>
        profesor.id ===
        disciplinas.find((d) => d.id === disciplinaId)?.profesorId
    );
    setProfesoresFiltrados(profesoresRelacionados);
  };

  const handleFiltrarAlumnos = useCallback(
    async (fecha: string, disciplinaId: number) => {
      try {
        const response = await api.get<AlumnoListadoResponse[]>(
          `/api/alumnos/por-fecha-y-disciplina?fecha=${fecha}&disciplinaId=${disciplinaId}`
        );
        setAlumnosFiltrados(response.data);
      } catch {
        toast.error("Error al cargar alumnos para la disciplina seleccionada.");
        setAlumnosFiltrados([]);
      }
    },
    []
  );

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
                    onChange={(e) => {
                      const id = e.target.value;
                      handleBuscar(id, (vals) => resetForm({ values: vals }));
                    }}
                  />
                  <Boton
                    onClick={() =>
                      handleBuscar(values.id?.toString() || "", (vals) =>
                        resetForm({ values: vals })
                      )
                    }
                    className="page-button"
                  >
                    <Search className="w-5 h-5 mr-2" />
                    Buscar
                  </Boton>
                </div>
              </div>

              <div className="mb-4">
                <label htmlFor="fecha" className="auth-label">
                  Fecha:
                </label>
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
              </div>

              <div className="mb-4">
                <label htmlFor="disciplinaId" className="auth-label">
                  Disciplina:
                </label>
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
                <ErrorMessage
                  name="disciplinaId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
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
                  {profesoresFiltrados.map((profesor: ProfesorResponse) => (
                    <option key={profesor.id} value={profesor.id}>
                      {profesor.nombre} {profesor.apellido}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="profesorId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
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
                  {alumnosFiltrados.map((alumno) => (
                    <option key={alumno.id} value={alumno.id}>
                      {alumno.nombre} {alumno.apellido}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="alumnoId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="col-span-full mb-4">
                <label htmlFor="observacion" className="auth-label">
                  Observaciones:
                </label>
                <Field
                  as="textarea"
                  id="observacion"
                  name="observacion"
                  className="form-input h-24"
                />
                <ErrorMessage
                  name="observacion"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="col-span-full mb-4">
                <label className="flex items-center space-x-2">
                  <Field
                    type="checkbox"
                    name="presente"
                    className="form-checkbox"
                  />
                  <span>Presente</span>
                </label>
              </div>
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
                onClick={() => {
                  resetForm();
                  setAlumnosFiltrados(alumnos);
                }}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
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
