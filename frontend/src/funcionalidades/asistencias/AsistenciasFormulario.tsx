import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { asistenciaEsquema } from "../../validaciones/asistenciaEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import {
  AsistenciaRequest,
  AsistenciaResponse,
  AlumnoListadoResponse,
  DisciplinaResponse,
} from "../../types/types";
import Boton from "../../componentes/comunes/Boton";

const AsistenciasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [alumnos, setAlumnos] = useState<AlumnoListadoResponse[]>([]);
  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [alumnosFiltrados, setAlumnosFiltrados] = useState<
    AlumnoListadoResponse[]
  >([]);
  const [disciplinaSeleccionada, setDisciplinaSeleccionada] = useState<
    number | null
  >(null);

  useEffect(() => {
    if (disciplinaSeleccionada) {
      handleFiltrarAlumnos(initialValues.fecha, disciplinaSeleccionada);
    }
  }, [disciplinaSeleccionada]);

  const initialValues: AsistenciaRequest = {
    fecha: "",
    alumnoId: 0,
    disciplinaId: 0,
    presente: true,
    observacion: "",
  };

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

  const handleBuscar = useCallback(async (idStr: string, setValues: any) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID inválido");
        return;
      }
      const data: AsistenciaResponse = await api.get(
        `/api/asistencias/${idNum}`
      );
      setValues({
        fecha: data.fecha,
        alumnoId: data.alumnoId,
        disciplinaId: data.disciplinaId,
        presente: data.presente,
        observacion: data.observacion || "",
      });
      toast.success("Asistencia cargada correctamente.");
    } catch {
      toast.error("Error al cargar la asistencia.");
    }
  }, []);

  const handleGuardarAsistencia = async (values: AsistenciaRequest) => {
    try {
      await api.post("/api/asistencias", values);
      toast.success("Asistencia registrada correctamente.");
    } catch {
      toast.error("Error al registrar la asistencia.");
    }
  };

  const handleFiltrarAlumnos = async (fecha: string, disciplinaId: number) => {
    try {
      const response = await api.get<AlumnoListadoResponse[]>(
        `/api/alumnos/por-fecha-y-disciplina?fecha=${fecha}&disciplinaId=${disciplinaId}`
      );
      setAlumnosFiltrados(response.data);
    } catch {
      toast.error("Error al cargar alumnos para la disciplina seleccionada.");
      setAlumnosFiltrados([]);
    }
  };

  return (
    <div className="formulario">
      <h1 className="form-title">Registro de Asistencias</h1>

      <Formik
        initialValues={initialValues}
        validationSchema={asistenciaEsquema}
        onSubmit={handleGuardarAsistencia}
      >
        {({ setValues, isSubmitting, values }) => (
          <Form className="formulario">
            {/* Selección de Fecha */}
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
                  setDisciplinaSeleccionada(null);
                  setAlumnosFiltrados(alumnos);
                }}
              />
              <ErrorMessage name="fecha" component="div" className="error" />
            </div>

            {/* Selección de Disciplina */}
            <div className="form-grid">
              <label>Disciplina:</label>
              <Field
                as="select"
                name="disciplinaId"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                  const id = Number(e.target.value);
                  setValues({ ...values, disciplinaId: id, alumnoId: 0 });
                  setDisciplinaSeleccionada(id);
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

            {/* Selección de Alumno */}
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

            {/* Observaciones */}
            <div className="form-grid">
              <label>Observaciones:</label>
              <Field as="textarea" name="observacion" className="form-input" />
              <ErrorMessage
                name="observacion"
                component="div"
                className="error"
              />
            </div>

            {/* Presente Checkbox */}
            <div className="form-checkbox">
              <label>
                <Field type="checkbox" name="presente" /> Presente
              </label>
            </div>

            {/* Botones de Acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar Asistencia
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => {
                  setValues(initialValues);
                  setDisciplinaSeleccionada(null);
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
