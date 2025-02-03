import { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import asistenciasApi from "../../utilidades/asistenciasApi";
import { toast } from "react-toastify";
import type {
  AsistenciaRequest,
  AlumnoListadoResponse,
  DisciplinaResponse,
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

  const [asistenciaId, setAsistenciaId] = useState<number | null>(null);
  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");

  const fetchData = useCallback(async () => {
    try {
      const [alumnosData, disciplinasData] = await Promise.all([
        asistenciasApi.obtenerAlumnosListado(),
        asistenciasApi.obtenerDisciplinas(),
      ]);
      setAlumnos(alumnosData);
      setDisciplinas(disciplinasData);
    } catch {
      toast.error("Error al cargar datos iniciales.");
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
    callback: (vals: AsistenciaRequest) => void
  ) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID inválido");
        return;
      }
      const asistencia = await asistenciasApi.obtenerAsistenciaPorId(idNum);
      callback(asistencia);
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

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      <Formik
        initialValues={initialAsistenciaValues}
        validationSchema={asistenciaEsquema}
        onSubmit={handleGuardarAsistencia}
        enableReinitialize
      >
        {({ resetForm, isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Búsqueda por ID */}
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Asistencia:
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    id="idBusqueda"
                    value={idBusqueda}
                    onChange={(e) => setIdBusqueda(e.target.value)}
                    className="form-input flex-grow"
                  />
                  <Boton
                    onClick={() =>
                      handleBuscar(idBusqueda, (vals) =>
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

              {/* Datos de la Asistencia */}
              <div className="mb-4">
                <label htmlFor="fecha" className="auth-label">
                  Fecha:
                </label>
                <Field
                  type="date"
                  id="fecha"
                  name="fecha"
                  className="form-input"
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
                <ErrorMessage
                  name="alumnoId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
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

            {mensaje && <p className="form-mensaje">{mensaje}</p>}
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AsistenciasFormulario;
