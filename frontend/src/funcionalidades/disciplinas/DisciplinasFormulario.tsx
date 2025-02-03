import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { disciplinaEsquema } from "../../validaciones/disciplinaEsquema";
import disciplinasApi from "../../utilidades/disciplinasApi";
import profesoresApi from "../../utilidades/profesoresApi";
import { toast } from "react-toastify";
import type { DisciplinaRequest, ProfesorResponse } from "../../types/types";
import Boton from "../../componentes/comunes/Boton";
import { Search } from "lucide-react";

const initialDisciplinaValues: DisciplinaRequest = {
  id: 0,
  nombre: "",
  horario: "",
  frecuenciaSemanal: 0,
  duracion: "",
  salon: "",
  valorCuota: 0,
  matricula: 0,
  profesorId: 0,
  activo: true,
};

const DisciplinasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [disciplinaId, setDisciplinaId] = useState<number | null>(null);
  const [profesores, setProfesores] = useState<ProfesorResponse[]>([]);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");

  useEffect(() => {
    const fetchProfesores = async () => {
      try {
        const response = await profesoresApi.listarProfesores();
        setProfesores(response as unknown as ProfesorResponse[]);
      } catch {
        toast.error("Error al cargar la lista de profesores.");
      }
    };
    fetchProfesores();
  }, []);

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) {
      handleBuscar(id, () => {});
    }
  }, [searchParams]);

  const handleBuscar = async (
    idStr: string,
    callback: (vals: DisciplinaRequest) => void
  ) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        setMensaje("ID inválido");
        return;
      }
      const disciplina = await disciplinasApi.obtenerDisciplinaPorId(idNum);
      callback({ ...disciplina, profesorId: disciplina.profesorId ?? 0 }); // ✅ Corrección aplicada
      setDisciplinaId(disciplina.id);
      setMensaje("");
    } catch {
      setMensaje("Disciplina no encontrada.");
      callback(initialDisciplinaValues);
      setDisciplinaId(null);
    }
  };

  const handleGuardarDisciplina = async (values: DisciplinaRequest) => {
    try {
      if (disciplinaId) {
        await disciplinasApi.actualizarDisciplina(disciplinaId, values);
        setMensaje("Disciplina actualizada correctamente.");
      } else {
        const nuevaDisciplina = await disciplinasApi.crearDisciplina(values);
        setDisciplinaId(nuevaDisciplina.id);
        setMensaje("Disciplina creada correctamente.");
      }
    } catch {
      setMensaje("Error al guardar la disciplina.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Formulario de Disciplinas</h1>
      <Formik
        initialValues={initialDisciplinaValues}
        validationSchema={disciplinaEsquema}
        onSubmit={handleGuardarDisciplina}
        enableReinitialize
      >
        {({ resetForm, isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Búsqueda por ID */}
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Disciplina:
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

              {/* Datos de la Disciplina */}
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
                <div key={name} className="mb-4">
                  <label htmlFor={name} className="auth-label">
                    {label}:
                  </label>
                  <Field
                    name={name}
                    type={type}
                    id={name}
                    className="form-input"
                  />
                  <ErrorMessage
                    name={name}
                    component="div"
                    className="auth-error"
                  />
                </div>
              ))}

              {/* Selección de Profesor */}
              <div className="mb-4">
                <label htmlFor="profesorId" className="auth-label">
                  Profesor:
                </label>
                <Field
                  as="select"
                  name="profesorId"
                  id="profesorId"
                  className="form-input"
                >
                  <option value="">Seleccione un Profesor</option>
                  {profesores.map((prof) => (
                    <option key={prof.id} value={prof.id}>
                      {prof.nombre} {prof.apellido}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="profesorId"
                  component="div"
                  className="auth-error"
                />
              </div>
            </div>

            <div className="form-acciones">
              <Boton
                type="submit"
                disabled={isSubmitting}
                className="page-button"
              >
                Guardar Disciplina
              </Boton>
              <Boton
                type="reset"
                onClick={() => resetForm({ values: initialDisciplinaValues })}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                onClick={() => navigate("/disciplinas")}
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

export default DisciplinasFormulario;
