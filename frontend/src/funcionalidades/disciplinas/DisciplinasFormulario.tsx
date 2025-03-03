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

// Tipos
import type {
  DisciplinaRegistroRequest,
  DisciplinaModificacionRequest,
  SalonResponse,
  ProfesorListadoResponse,
  DisciplinaHorarioRequest,
  DisciplinaDetalleResponse
} from "../../types/types";

const diasSemana: string[] = ["LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO"];

const initialDisciplinaValues: DisciplinaRegistroRequest & Partial<DisciplinaModificacionRequest> = {
  nombre: "",
  salonId: 0,
  profesorId: 0,
  recargoId: undefined,
  valorCuota: 0,
  matricula: 0,
  claseSuelta: undefined,
  clasePrueba: undefined,
  activo: true,
  horarios: [] as DisciplinaHorarioRequest[],
};

const disciplinaSchema = Yup.object().shape({
  nombre: Yup.string().required("El nombre es obligatorio"),
  salonId: Yup.number().positive().required("Debe seleccionar un salón"),
  profesorId: Yup.number().positive().required("Debe seleccionar un profesor"),
  valorCuota: Yup.number().positive().required("El valor de la cuota es obligatorio"),
  matricula: Yup.number().min(0).required("El valor de la matrícula es obligatorio"),
  activo: Yup.boolean().required("El estado activo es obligatorio"),
  horarios: Yup.array()
    .of(
      Yup.object().shape({
        // El campo "id" es opcional para identificar elementos existentes
        id: Yup.number().optional(),
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
  const [matricula, setMatricula] = useState<number>(0);

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

  const fetchMatricula = useCallback(async () => {
    try {
      const conceptos = await conceptosApi.listarConceptos();
      const matriculaConcepto = conceptos.find(
        (concepto) => concepto.descripcion.toUpperCase() === "MATRICULA"
      );
      if (matriculaConcepto) {
        setMatricula(matriculaConcepto.precio);
      } else {
        setMatricula(0);
      }
    } catch (error) {
      console.error("Error al obtener el concepto 'MATRICULA':", error);
    }
  }, []);

  const mapDetalleToFormValues = (detalle: any) => ({
    nombre: detalle.nombre,
    salonId: detalle.salonId, // Asegúrate de que este campo existe en la respuesta real
    profesorId: detalle.profesorId ?? 0,
    recargoId: detalle.recargoId,
    valorCuota: detalle.valorCuota,
    matricula: detalle.matricula,
    claseSuelta: detalle.claseSuelta,
    clasePrueba: detalle.clasePrueba,
    activo: detalle.activo,
    horarios: detalle.horarios, // Se espera que la API envíe este arreglo
  });

  useEffect(() => {
    fetchMatricula();
    const idParam = searchParams.get("id");
    if (idParam) {
      const fetchDisciplina = async () => {
        try {
          const detalle: DisciplinaDetalleResponse = await disciplinasApi.obtenerDisciplinaPorId(Number(idParam));
          const disciplinaForm = mapDetalleToFormValues(detalle);
          setFormValues(disciplinaForm);
          setDisciplinaId(detalle.id);
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

  const handleSubmit = useCallback(
    async (values: DisciplinaRegistroRequest) => {
      try {
        const payload: DisciplinaRegistroRequest & Partial<DisciplinaModificacionRequest> = {
          ...values,
          matricula,
        };
        if (disciplinaId) {
          await disciplinasApi.actualizarDisciplina(
            disciplinaId,
            { ...payload, activo: true } as DisciplinaModificacionRequest
          );
          toast.success("Disciplina actualizada correctamente.");
        } else {
          const nuevo = await disciplinasApi.registrarDisciplina(payload);
          setDisciplinaId(nuevo.id);
          toast.success("Disciplina creada correctamente.");
        }
        setMensaje("Disciplina guardada exitosamente.");
        navigate("/disciplinas");
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

      {/* Nuevo bloque para mostrar el ID de la Disciplina */}
      {disciplinaId !== null && (
        <div className="mb-4">
          <label htmlFor="disciplinaId" className="auth-label">ID de la Disciplina:</label>
          <Field name="id" type="number" className="form-input" readOnly />
        </div>
      )}

      <div className="mb-4">
        <label htmlFor="idBusqueda" className="auth-label">ID de Disciplina:</label>
        <div className="flex gap-2">
          <input
            type="number"
            id="idBusqueda"
            value={idBusqueda}
            onChange={(e) => setIdBusqueda(e.target.value)}
            className="form-input flex-grow"
          />
          <Boton onClick={() => { /* Lógica de búsqueda manual si se requiere */ }} className="page-button">
            <Search className="w-5 h-5 mr-2" /> Buscar
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
        {({ isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Campos básicos */}
              <div className="mb-4">
                <label htmlFor="nombre" className="auth-label">Nombre:</label>
                <Field name="nombre" type="text" className="form-input" />
                <ErrorMessage name="nombre" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="salonId" className="auth-label">Salón:</label>
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
                <label htmlFor="profesorId" className="auth-label">Profesor:</label>
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
                <label htmlFor="valorCuota" className="auth-label">Valor de Cuota:</label>
                <Field name="valorCuota" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="valorCuota" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="claseSuelta" className="auth-label">Clase Suelta:</label>
                <Field name="claseSuelta" type="number" step="0.01" className="form-input" />
                <ErrorMessage name="claseSuelta" component="div" className="auth-error" />
              </div>
              <div className="mb-4">
                <label htmlFor="clasePrueba" className="auth-label">Clase de Prueba:</label>
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
              {/* FieldArray para Horarios */}
              <div className="mb-4 col-span-1 sm:col-span-2">
                <label className="auth-label">Horarios de Clase:</label>
                <FieldArray name="horarios">
                  {({ push, remove }) => (
                    <div>
                      {values.horarios && values.horarios.length > 0 ? (
                        values.horarios.map((horario: DisciplinaHorarioRequest, index: number) => (
                          <div key={index} className="horario-item border p-2 mb-2">
                            {horario.id !== undefined && (
                              <Field name={`horarios.${index}.id`} type="hidden" />
                            )}
                            <div className="mb-2">
                              <label>Dia:</label>
                              <Field as="select" name={`horarios.${index}.diaSemana`} className="form-input">
                                <option value="">Seleccione</option>
                                {diasSemana.map((dia) => (
                                  <option key={dia} value={dia}>
                                    {dia}
                                  </option>
                                ))}
                              </Field>
                              <ErrorMessage name={`horarios.${index}.diaSemana`} component="div" className="auth-error" />
                            </div>
                            <div className="mb-2">
                              <label>Horario Inicio:</label>
                              <Field name={`horarios.${index}.horarioInicio`} type="time" className="form-input" />
                              <ErrorMessage name={`horarios.${index}.horarioInicio`} component="div" className="auth-error" />
                            </div>
                            <div className="mb-2">
                              <label>Duración (horas):</label>
                              <Field name={`horarios.${index}.duracion`} type="number" step="0.1" className="form-input" />
                              <ErrorMessage name={`horarios.${index}.duracion`} component="div" className="auth-error" />
                            </div>
                            <div className="flex justify-end">
                              <Boton type="button" onClick={() => remove(index)} className="page-button-secondary">
                                Eliminar Horario
                              </Boton>
                            </div>
                          </div>
                        ))
                      ) : (
                        <div>No hay horarios agregados.</div>
                      )}
                      <Boton
                        type="button"
                        onClick={() =>
                          push({
                            diaSemana: "",
                            horarioInicio: "",
                            duracion: 0,
                          })
                        }
                        className="page-button"
                      >
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
