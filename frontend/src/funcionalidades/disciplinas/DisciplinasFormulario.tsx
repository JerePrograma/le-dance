import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { disciplinaEsquema } from "../../validaciones/disciplinaEsquema";
import api from "../../utilidades/axiosConfig";
import { toast } from "react-toastify";
import {
  DisciplinaRequest,
  DisciplinaResponse,
  ProfesorResponse,
} from "../../types/types";
import Boton from "../../componentes/comunes/Boton";

const DisciplinasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [profesores, setProfesores] = useState<ProfesorResponse[]>([]);

  const initialValues: DisciplinaRequest = {
    id: 0,
    nombre: "",
    horario: "",
    frecuenciaSemanal: 0,
    duracion: "",
    salon: "",
    valorCuota: 0,
    matricula: 0,
    profesorId: 0,
  };

  useEffect(() => {
    const fetchProfesores = async () => {
      try {
        const response = await api.get<ProfesorResponse[]>(
          "/api/profesores/simplificados"
        );
        setProfesores(response.data);
      } catch {
        toast.error("Error al cargar la lista de profesores.");
      }
    };
    fetchProfesores();
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
      const data: DisciplinaResponse = await api.get(
        `/api/disciplinas/${idNum}`
      );
      setValues({
        nombre: data.nombre,
        horario: data.horario,
        frecuenciaSemanal: data.frecuenciaSemanal || 0,
        duracion: data.duracion,
        salon: data.salon,
        valorCuota: data.valorCuota || 0,
        matricula: data.matricula || 0,
        profesorId: data.profesorId || 0,
      });
      toast.success("Disciplina cargada correctamente.");
    } catch {
      toast.error("Error al cargar la disciplina.");
      setValues(initialValues);
    }
  }, []);

  const handleGuardarDisciplina = async (values: DisciplinaRequest) => {
    try {
      if (values.id) {
        await api.put(`/api/disciplinas/${values.id}`, values);
        toast.success("Disciplina actualizada correctamente.");
      } else {
        await api.post("/api/disciplinas", values);
        toast.success("Disciplina creada correctamente.");
      }
    } catch {
      toast.error("Error al guardar la disciplina.");
    }
  };

  return (
    <div className="formulario">
      <h1 className="form-title">Formulario de Disciplinas</h1>

      <Formik
        initialValues={initialValues}
        validationSchema={disciplinaEsquema}
        onSubmit={handleGuardarDisciplina}
      >
        {({ setValues, isSubmitting }) => (
          <Form className="formulario">
            {/* Búsqueda por ID */}
            <div className="form-busqueda">
              <label htmlFor="idBusqueda">Número de Disciplina:</label>
              <Field
                type="number"
                id="idBusqueda"
                name="id"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  const idValue = e.target.value ? Number(e.target.value) : 0; // Asegura que siempre sea un número
                  setValues((prevValues) => ({
                    ...prevValues,
                    id: idValue, // Siempre un número, evitando `undefined`
                  }));
                }}
              />

              <Boton
                type="button"
                onClick={() =>
                  handleBuscar(String(initialValues.id), setValues)
                }
              >
                Buscar
              </Boton>
            </div>

            {/* Campos del Formulario */}
            <fieldset className="form-fieldset">
              <legend>Datos de la Disciplina</legend>
              <div className="form-grid">
                {[
                  { name: "nombre", label: "Nombre (obligatorio)" },
                  {
                    name: "horario",
                    label: "Horario (Ej. Lunes y Miércoles 18:00 - 19:00)",
                  },
                  {
                    name: "frecuenciaSemanal",
                    label: "Frecuencia Semanal",
                    type: "number",
                  },
                  { name: "duracion", label: "Duración (Ej. 1 hora)" },
                  { name: "salon", label: "Salón (Ej. Salón A)" },
                  { name: "valorCuota", label: "Valor Cuota", type: "number" },
                  { name: "matricula", label: "Matrícula", type: "number" },
                  {
                    name: "cupoMaximo",
                    label: "Cupo Máximo de Alumnos",
                    type: "number",
                  },
                ].map(({ name, label, type = "text" }) => (
                  <div key={name}>
                    <label>{label}:</label>
                    <Field name={name} type={type} className="form-input" />
                    <ErrorMessage
                      name={name}
                      component="div"
                      className="error"
                    />
                  </div>
                ))}

                {/* Selección de Profesor */}
                <div>
                  <label>Profesor:</label>
                  <Field as="select" name="profesorId" className="form-input">
                    <option value="">Seleccione un Profesor</option>
                    {profesores.map((prof) => (
                      <option key={prof.id} value={prof.id}>
                        {prof.nombre + " " + prof.apellido}
                      </option>
                    ))}
                  </Field>
                  <ErrorMessage
                    name="profesorId"
                    component="div"
                    className="error"
                  />
                </div>
              </div>
            </fieldset>

            {/* Botones de Acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                Guardar Disciplina
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() => setValues(initialValues)}
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                secondary
                onClick={() => navigate("/disciplinas")}
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

export default DisciplinasFormulario;
