// src/funcionalidades/disciplinas/DisciplinasFormulario.tsx
import React, { useState, useEffect, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, FieldArray, ErrorMessage } from "formik";
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
  DisciplinaHorarioRequest
} from "../../types/types";

// Lista de días (puedes ajustar según tus necesidades)
const diasSemana: DiaSemana[] = [
  "LUNES",
  "MARTES",
  "MIERCOLES",
  "JUEVES",
  "VIERNES",
  "SABADO",
  "DOMINGO",
];

// Valores iniciales para el formulario de disciplina.
// Se han eliminado los campos individuales para horarios y se agrega "horarios" como arreglo.
const initialDisciplinaValues: DisciplinaRegistroRequest & Partial<DisciplinaModificacionRequest> = {
  nombre: "",
  salonId: 0,
  profesorId: 0,
  recargoId: undefined,
  valorCuota: 0,
  matricula: 0, // Se fijará automáticamente con el valor del concepto "MATRICULA"
  claseSuelta: undefined,
  clasePrueba: undefined,
  activo: true,
  // Nuevo campo: lista de horarios (vacío por defecto)
  horarios: [],
};

const disciplinaSchema = Yup.object().shape({
  nombre: Yup.string().required("El nombre es obligatorio"),
  salonId: Yup.number().positive().required("Debe seleccionar un salón"),
  profesorId: Yup.number().positive().required("Debe seleccionar un profesor"),
  valorCuota: Yup.number().positive().required("El valor de la cuota es obligatorio"),
  matricula: Yup.number().min(0).required("El valor de la matrícula es obligatorio"),
  activo: Yup.boolean().required("El estado activo es obligatorio"),
  // Validación para el arreglo de horarios: al menos uno debe existir y cada uno debe tener sus campos
  horarios: Yup.array()
    .of(
      Yup.object().shape({
        diaSemana: Yup.string().required("El día es obligatorio"),
        horarioInicio: Yup.string().required("El horario de inicio es obligatorio"),
        duracion: Yup.number().positive().required("La duración es obligatoria"),
      })
    )
    .min(1, "Debe ingresar al menos un horario"),
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
          // Asumimos que el backend ahora retorna "horarios" como arreglo de objetos
          setFormValues({
            nombre: disciplina.nombre,
            salonId: disciplina.salonId,
            profesorId: disciplina.profesorId,
            recargoId: disciplina.recargoId,
            valorCuota: disciplina.valorCuota,
            matricula: disciplina.matricula,
            claseSuelta: disciplina.claseSuelta,
            clasePrueba: disciplina.clasePrueba,
            activo: disciplina.activo,
            horarios: disciplina.horarios, // Se espera un arreglo de horarios
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
  }, [searchParams, fetchMatricula]);

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
        initialValues={{ ...formValues, matricula }}
        validationSchema={disciplinaSchema}
        onSubmit={handleSubmit}
        enableReinitialize
      >
        {({ values, isSubmitting }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Campo para el nombre */}
              <div className="mb-4">
                <label htmlFor="nombre" className="auth-label">
                  Nombre:
                </label>
                <Field name="nombre" type="text" className="form-input" />
                <ErrorMessage name="nombre" component="div" className="auth-error" />
              </div>

              {/* Campo para seleccionar Salón */}
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

              {/* Campo para seleccionar Profesor */}
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

              {/* Campo para Valor de Cuota */}
              <div className="mb-4">
                <label htmlFor="valorCuota" className="auth-label">
                  Valor de Cuota:
                </label>
                <Field name="valorCuota" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="valorCuota" component="div" className="auth-error" />
              </div>

              {/* Campo para Clase Suelta */}
              <div className="mb-4">
                <label htmlFor="claseSuelta" className="auth-label">
                  Clase Suelta:
                </label>
                <Field name="claseSuelta" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="claseSuelta" component="div" className="auth-error" />
              </div>

              {/* Campo para Clase de Prueba */}
              <div className="mb-4">
                <label htmlFor="clasePrueba" className="auth-label">
                  Clase de Prueba:
                </label>
                <Field name="clasePrueba" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="clasePrueba" component="div" className="auth-error" />
              </div>

              {/* Checkbox para activo (solo en edición) */}
              {disciplinaId !== null && (
                <div className="mb-4">
                  <label className="flex items-center">
                    <Field type="checkbox" name="activo" className="form-checkbox" />
                    <span className="ml-2">Activo</span>
                  </label>
                  <ErrorMessage name="activo" component="div" className="auth-error" />
                </div>
              )}

              {/* Campo para Horarios: usando FieldArray */}
              <div className="mb-4 col-span-1 sm:col-span-2">
                <label className="auth-label">Horarios de Clase:</label>
                <FieldArray name="horarios">
                  {({ push, remove, form }) => (
                    <div>
                      {form.values.horarios && form.values.horarios.length > 0 ? (
                        form.values.horarios.map((horario: any, index: number) => (
                          <div key={index} className="flex flex-col sm:flex-row gap-2 items-end mb-2 border p-2 rounded">
                            <div className="flex-1">
                              <label className="block text-sm font-medium">Día</label>
                              <Field as="select" name={`horarios.${index}.diaSemana`} className="form-input">
                                <option value="">Seleccione un día</option>
                                {diasSemana.map((dia) => (
                                  <option key={dia} value={dia}>
                                    {dia}
                                  </option>
                                ))}
                              </Field>
                              <ErrorMessage name={`horarios.${index}.diaSemana`} component="div" className="text-red-500 text-xs" />
                            </div>
                            <div className="flex-1">
                              <label className="block text-sm font-medium">Horario de Inicio</label>
                              <Field name={`horarios.${index}.horarioInicio`} type="time" className="form-input" />
                              <ErrorMessage name={`horarios.${index}.horarioInicio`} component="div" className="text-red-500 text-xs" />
                            </div>
                            <div className="flex-1">
                              <label className="block text-sm font-medium">Duración (horas)</label>
                              <Field name={`horarios.${index}.duracion`} type="number" step="0.5" className="form-input" />
                              <ErrorMessage name={`horarios.${index}.duracion`} component="div" className="text-red-500 text-xs" />
                            </div>
                            <div>
                              <Boton type="button" onClick={() => remove(index)} className="bg-red-500 text-white px-2 py-1 rounded">
                                Eliminar
                              </Boton>
                            </div>
                          </div>
                        ))
                      ) : (
                        <p className="text-sm text-gray-500">No se han agregado horarios.</p>
                      )}
                      <Boton type="button" onClick={() => push({ diaSemana: "", horarioInicio: "", duracion: 1 })} className="bg-blue-500 text-white px-2 py-1 rounded mt-2">
                        Agregar Horario
                      </Boton>
                    </div>
                  )}
                </FieldArray>
              </div>
            </div>

            <div className="form-acciones">
              <Boton type="submit" className="page-button" disabled={isSubmitting}>
                {disciplinaId ? "Actualizar" : "Crear"} Disciplina
              </Boton>
              <Boton type="button" onClick={() => navigate("/disciplinas")} className="page-button-secondary">
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
