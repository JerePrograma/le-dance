// src/funcionalidades/disciplinas/DisciplinasFormulario.tsx
import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { toast } from "react-toastify";
import { Search } from "lucide-react";
import Boton from "../../componentes/comunes/Boton";

// APIs
import disciplinasApi from "../../api/disciplinasApi";
import salonesApi from "../../api/salonesApi";
import profesoresApi from "../../api/profesoresApi";
import conceptosApi from "../../api/conceptosApi";

// Tipos (ajusta según tu proyecto)
import type {
  DisciplinaRegistroRequest,
  DisciplinaModificacionRequest,
  SalonResponse,
  ProfesorListadoResponse,
  DiaSemana,
  DisciplinaResponse,
} from "../../types/types";

// Lista de días (puedes ajustar según tus necesidades)
const diasSemana: DiaSemana[] = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
];

// Valores iniciales para el formulario de disciplina.  
// Se incluyen todos los campos obligatorios.
const initialDisciplinaValues: DisciplinaRegistroRequest & Partial<DisciplinaModificacionRequest> = {
  nombre: "",
  diasSemana: [],
  frecuenciaSemanal: 1,
  horarioInicio: "",
  duracion: 1,
  salonId: 0,
  profesorId: 0,
  recargoId: undefined,
  valorCuota: 0,
  matricula: 0, // Se fijará automáticamente con el valor del concepto "MATRICULA"
  claseSuelta: undefined,
  clasePrueba: undefined,
  // Para modificación, se requiere 'activo'
  activo: true,
};

const disciplinaSchema = Yup.object().shape({
  nombre: Yup.string().required("El nombre es obligatorio"),
  diasSemana: Yup.array().of(Yup.string()).min(1, "Seleccione al menos un día"),
  frecuenciaSemanal: Yup.number().positive().integer().required("La frecuencia semanal es obligatoria"),
  horarioInicio: Yup.string().required("El horario de inicio es obligatorio"),
  duracion: Yup.number().positive().required("La duración es obligatoria"),
  salonId: Yup.number().positive().required("Debe seleccionar un salón"),
  profesorId: Yup.number().positive().required("Debe seleccionar un profesor"),
  valorCuota: Yup.number().positive().required("El valor de la cuota es obligatorio"),
  matricula: Yup.number().min(0).required("El valor de la matrícula es obligatorio"),
  activo: Yup.boolean().required("El estado activo es obligatorio"),
});

const DisciplinasFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [disciplinaId, setDisciplinaId] = useState<number | null>(null);
  const [formValues, setFormValues] = useState(initialDisciplinaValues);
  const [mensaje, setMensaje] = useState("");
  const [idBusqueda, setIdBusqueda] = useState("");

  const [salones, setSalones] = useState<SalonResponse[]>([]);
  const [profesores, setProfesores] = useState<ProfesorListadoResponse[]>([]);

  // Estado para el valor de matrícula obtenido desde el Concepto "MATRICULA"
  const [matricula, setMatricula] = useState<number>(0);

  // Cargar salones y profesores
  const fetchSalones = useCallback(async () => {
    try {
      const response = await salonesApi.listarSalones();
      setSalones(response.content);
    } catch (error) {
      console.error("Error al cargar salones:", error);
      toast.error("Error al cargar salones");
    }
  }, []);

  const fetchProfesores = useCallback(async () => {
    try {
      const response = await profesoresApi.listarProfesoresActivos();
      setProfesores(response);
    } catch (error) {
      console.error("Error al cargar profesores:", error);
      toast.error("Error al cargar profesores");
    }
  }, []);

  useEffect(() => {
    fetchSalones();
    fetchProfesores();
  }, [fetchSalones, fetchProfesores]);

  // Función para obtener la matrícula (valor del Concepto con descripción "MATRICULA")
  const fetchMatricula = useCallback(async () => {
    try {
      const conceptos = await conceptosApi.listarConceptos();
      const matriculaConcepto = conceptos.find(
        (concepto) => concepto.descripcion.toUpperCase() === "MATRICULA"
      );
      if (matriculaConcepto) {
        console.log("Concepto 'MATRICULA' encontrado:", matriculaConcepto);
        setMatricula(matriculaConcepto.precio);
      } else {
        console.warn("No se encontró el concepto 'MATRICULA'.");
        setMatricula(0);
      }
    } catch (error) {
      console.error("Error al obtener el concepto 'MATRICULA':", error);
    }
  }, []);

  // Cargar la matrícula y, si hay id de disciplina en la URL, cargar la disciplina para edición
  useEffect(() => {
    fetchMatricula();
    const idParam = searchParams.get("id");
    if (idParam) {
      const fetchDisciplina = async () => {
        try {
          const disciplina: DisciplinaResponse = await disciplinasApi.obtenerDisciplinaPorId(Number(idParam));
          setFormValues({
            nombre: disciplina.nombre,
            diasSemana: disciplina.diasSemana, // Suponemos que ya es un string[]
            frecuenciaSemanal: disciplina.frecuenciaSemanal,
            horarioInicio: disciplina.horarioInicio,
            duracion: disciplina.duracion,
            salonId: salones.find((s) => s.nombre === disciplina.salon)?.id || 0,
            profesorId:
              profesores.find(
                (p) =>
                  p.nombre === disciplina.profesorNombre &&
                  p.apellido === disciplina.profesorApellido
              )?.id || 0,
            recargoId: disciplina.recargoId,
            valorCuota: disciplina.valorCuota,
            matricula: disciplina.matricula,
            claseSuelta: disciplina.claseSuelta,
            clasePrueba: disciplina.clasePrueba,
            activo: disciplina.activo,
          });
          setDisciplinaId(disciplina.id);
          setMensaje("Disciplina encontrada.");
        } catch (error) {
          console.error("Error al buscar la disciplina:", error);
          setMensaje("Disciplina no encontrada.");
          setFormValues(initialDisciplinaValues);
          setDisciplinaId(null);
        }
      };
      fetchDisciplina();
    }
  }, [searchParams, salones, profesores, fetchMatricula]);

  // Al enviar, forzamos que el valor de matrícula sea el obtenido desde el concepto "MATRICULA"
  const handleSubmit = useCallback(
    async (values: DisciplinaRegistroRequest) => {
      try {
        const payload: DisciplinaRegistroRequest & Partial<DisciplinaModificacionRequest> = {
          ...values,
          matricula, // Forzamos el valor de matrícula obtenido del concepto
        };
        if (disciplinaId) {
          await disciplinasApi.actualizarDisciplina(disciplinaId, {
            ...payload,
            activo: true,
          } as DisciplinaModificacionRequest);
          toast.success("Disciplina actualizada correctamente.");
        } else {
          const nuevo = await disciplinasApi.registrarDisciplina(payload);
          setDisciplinaId(nuevo.id);
          toast.success("Disciplina creada correctamente.");
        }
        setMensaje("Disciplina guardada exitosamente.");
      } catch (error) {
        console.error("Error al guardar la disciplina:", error);
        toast.error("Error al guardar la disciplina.");
        setMensaje("Error al guardar la disciplina.");
      }
    },
    [disciplinaId, matricula]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">Formulario de Disciplina</h1>
      <div className="mb-4">
        <label htmlFor="idBusqueda" className="auth-label">
          ID de Disciplina:
        </label>
        <div className="flex gap-2">
          <input
            type="number"
            id="idBusqueda"
            value={idBusqueda}
            onChange={(e) => setIdBusqueda(e.target.value)}
            className="form-input flex-grow"
          />
          <Boton onClick={() => { /* Lógica de búsqueda manual si se requiere */ }} className="page-button">
            <Search className="w-5 h-5 mr-2" />
            Buscar
          </Boton>
        </div>
      </div>

      {mensaje && (
        <p className={`form-mensaje ${mensaje.includes("Error") ? "form-mensaje-error" : "form-mensaje-success"}`}>
          {mensaje}
        </p>
      )}

      <Formik
        initialValues={{ ...formValues, matricula }} // Se actualiza matrícula desde el concepto "MATRICULA"
        validationSchema={disciplinaSchema}
        onSubmit={handleSubmit}
        enableReinitialize
      >
        {({ isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="mb-4">
                <label htmlFor="nombre" className="auth-label">
                  Nombre:
                </label>
                <Field name="nombre" type="text" className="form-input" />
                <ErrorMessage name="nombre" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label className="auth-label">Días de la semana:</label>
                <div className="flex flex-wrap gap-2">
                  {diasSemana.map((dia) => (
                    <label key={dia} className="flex items-center">
                      <Field
                        type="checkbox"
                        name="diasSemana"
                        value={dia}
                        className="form-checkbox"
                      />
                      <span className="ml-2">{dia}</span>
                    </label>
                  ))}
                </div>
                <ErrorMessage name="diasSemana" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="frecuenciaSemanal" className="auth-label">
                  Frecuencia Semanal:
                </label>
                <Field name="frecuenciaSemanal" type="number" className="form-input" />
                <ErrorMessage name="frecuenciaSemanal" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="horarioInicio" className="auth-label">
                  Horario de Inicio:
                </label>
                <Field name="horarioInicio" type="time" className="form-input" />
                <ErrorMessage name="horarioInicio" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="duracion" className="auth-label">
                  Duración (horas):
                </label>
                <Field name="duracion" type="number" step="0.5" className="form-input" />
                <ErrorMessage name="duracion" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="salonId" className="auth-label">
                  Salón:
                </label>
                <Field as="select" name="salonId" className="form-input">
                  <option value="">Seleccione un salón</option>
                  {salones.map((salon) => (
                    <option key={salon.id} value={salon.id}>
                      {salon.nombre}
                    </option>
                  ))}
                </Field>
                <ErrorMessage name="salonId" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="profesorId" className="auth-label">
                  Profesor:
                </label>
                <Field as="select" name="profesorId" className="form-input">
                  <option value="">Seleccione un profesor</option>
                  {profesores.map((profesor) => (
                    <option key={profesor.id} value={profesor.id}>
                      {profesor.nombre} {profesor.apellido}
                    </option>
                  ))}
                </Field>
                <ErrorMessage name="profesorId" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="valorCuota" className="auth-label">
                  Valor de Cuota:
                </label>
                <Field name="valorCuota" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="valorCuota" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="claseSuelta" className="auth-label">
                  Clase Suelta:
                </label>
                <Field name="claseSuelta" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="claseSuelta" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="clasePrueba" className="auth-label">
                  Clase de Prueba:
                </label>
                <Field name="clasePrueba" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="clasePrueba" component="div" className="auth-error" />
              </div>
              {disciplinaId !== null && (
                <div className="mb-4">
                  <label className="flex items-center">
                    <Field type="checkbox" name="activo" className="form-checkbox" />
                    <span className="ml-2">Activo</span>
                  </label>
                  <ErrorMessage name="activo" component="div" className="auth-error" />
                </div>
              )}
            </div>

            <div className="form-acciones">
              <Boton type="submit" className="page-button">
                {disciplinaId ? "Actualizar" : "Crear"} Disciplina
              </Boton>
              <Boton
                type="button"
                onClick={() => navigate("/disciplinas")}
                className="page-button-secondary"
              >
                Cancelar
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default DisciplinasFormulario;
