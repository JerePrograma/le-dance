// src/funcionalidades/asistencias/AsistenciasFormulario.tsx
import React, { useEffect, useState, useCallback } from "react";
import { Form, useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Formik } from "formik";
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import {
  AsistenciaRequest,
  AsistenciaResponse,
  AlumnoListadoResponse,
  DisciplinaResponse,
  ProfesorResponse,
} from "../../types/types";
import Boton from "../../componentes/comunes/Boton";

const initialAsistenciaValues: AsistenciaRequest = {
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
    const fetchProfesores = async () => {
      try {
        const response = await api.get<ProfesorResponse[]>("/api/profesores");
        setProfesores(response.data);
      } catch {
        toast.error("Error al cargar profesores.");
      }
    };

    fetchProfesores();
  }, []);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [alumnosResponse, disciplinasResponse] = await Promise.all([
          api.get<AlumnoListadoResponse[]>("/api/alumnos/listado"),
          api.get<DisciplinaResponse[]>("/api/disciplinas"),
        ]);
        setAlumnos(alumnosResponse.data);
        setAlumnosFiltrados(alumnosResponse.data);
        setDisciplinas(disciplinasResponse.data);
      } catch {
        toast.error("Error al cargar alumnos o disciplinas.");
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
      ...values, // ✅ Copiamos los valores actuales del formulario
      disciplinaId, // ✅ Actualizamos la disciplina seleccionada
      profesorId: 0, // ✅ Reset profesor cuando se cambia disciplina
    });

    // Filtrar profesores relacionados con la disciplina seleccionada
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

  // Hasta aquí se finaliza la parte de inicialización y lógica para AsistenciasFormulario

  return (
    <div className="formulario">
      <h1 className="form-title">Registro de Asistencias</h1>
      <Formik
        initialValues={initialAsistenciaValues}
        validationSchema={asistenciaEsquema}
        onSubmit={handleGuardarAsistencia}
        enableReinitialize
      >
        {({ resetForm, setValues, isSubmitting, values }) => (
          <Form className="formulario">
            <div className="form-grid">
              <label>Fecha:</label>
              <Field
                type="date"
                name="fecha"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  setValues({
                    ...values,
                    fecha: e.target.value,
                    disciplinaId: 0,
                    alumnoId: 0,
                  });
                  // Aunque "disciplinaSeleccionada" se actualiza, si no se usa en el render, se puede omitir.
                  setAlumnosFiltrados(alumnos);
                }}
              />
              <ErrorMessage name="fecha" component="div" className="error" />
            </div>
            <div className="form-grid">
              <label>Disciplina:</label>
              <Field
                as="select"
                name="disciplinaId"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                  const id = Number(e.target.value);
                  handleSeleccionarDisciplina(id, values, setValues); // ✅ Se pasa `values` correctamente
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
                className="error"
              />
            </div>
            <div className="form-grid">
              <label>Profesor:</label>
              <Field as="select" name="profesorId" className="form-input">
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
                className="error"
              />

              <ErrorMessage
                name="profesorId"
                component="div"
                className="error"
              />
            </div>
            <div className="form-grid">
              <label>Alumno:</label>
              <Field as="select" name="alumnoId" className="form-input">
                <option value="">Seleccione un alumno</option>
                {alumnosFiltrados.map((alumno) => (
                  <option key={alumno.id} value={alumno.id}>
                    {alumno.nombre} {alumno.apellido}
                  </option>
                ))}
              </Field>
              <ErrorMessage name="alumnoId" component="div" className="error" />
            </div>
            <div className="form-grid">
              <label>Observaciones:</label>
              <Field as="textarea" name="observacion" className="form-input" />
              <ErrorMessage
                name="observacion"
                component="div"
                className="error"
              />
            </div>
            <div className="form-checkbox">
              <label>
                <Field type="checkbox" name="presente" /> Presente
              </label>
            </div>
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar Asistencia
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => {
                  resetForm();
                  setAlumnosFiltrados(alumnos);
                }}
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                secondary
                onClick={() => navigate("/asistencias")}
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
