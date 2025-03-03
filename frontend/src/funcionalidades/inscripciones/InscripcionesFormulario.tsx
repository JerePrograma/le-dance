"use client";

import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";

// APIs
import inscripcionesApi from "../../api/inscripcionesApi";
import disciplinasApi from "../../api/disciplinasApi";
import bonificacionesApi from "../../api/bonificacionesApi";

// Types
import type {
  InscripcionRegistroRequest,
  InscripcionResponse,
  BonificacionResponse,
  DisciplinaDetalleResponse,
} from "../../types/types";

// Esquema de validación
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";

// Extendemos el tipo para el formulario (para distinguir inscripciones ya existentes)
interface InscripcionFormData extends InscripcionRegistroRequest {
  id?: number;
}

// Valor inicial para una inscripción nueva
const initialInscripcion: InscripcionFormData = {
  alumnoId: 0,
  inscripcion: {
    disciplinaId: 0,
    bonificacionId: undefined,
  },
  fechaInscripcion: new Date().toISOString().split("T")[0],
};

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Listados de catálogos
  const [disciplinas, setDisciplinas] = useState<DisciplinaDetalleResponse[]>([]);
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>([]);

  // Para el alumno (se asume que viene en la URL)
  const [alumnoId, setAlumnoId] = useState<number>(0);

  // Lista dinámica de inscripciones a agregar/editar
  const [inscripcionesList, setInscripcionesList] = useState<InscripcionFormData[]>([{ ...initialInscripcion }]);

  // Lista de inscripciones previas (ya guardadas) del alumno
  const [prevInscripciones, setPrevInscripciones] = useState<InscripcionResponse[]>([]);

  // Cargar catálogos de disciplinas y bonificaciones
  useEffect(() => {
    const fetchCatalogos = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ]);
        setDisciplinas(discData || []);
        setBonificaciones(bonData || []);
      } catch (error) {
        toast.error("Error al cargar disciplinas o bonificaciones.");
      }
    };
    fetchCatalogos();
  }, []);

  // Leer "alumnoId" de la URL y asignarlo a los formularios
  useEffect(() => {
    const alumnoParam = searchParams.get("alumnoId");
    if (alumnoParam) {
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        setAlumnoId(aId);
        setInscripcionesList((prev) =>
          prev.map((insc) => ({ ...insc, alumnoId: aId }))
        );
      }
    }
  }, [searchParams]);

  // Función para cargar las inscripciones previas del alumno
  const fetchPrevInscripciones = async () => {
    if (alumnoId) {
      try {
        const lista: InscripcionResponse[] = await inscripcionesApi.listar(alumnoId);
        setPrevInscripciones(lista);
      } catch (error) {
        toast.error("Error al cargar inscripciones previas.");
      }
    }
  };

  useEffect(() => {
    fetchPrevInscripciones();
  }, [alumnoId]);

  // Función para agregar una nueva inscripción (formulario vacío)
  const agregarInscripcion = () => {
    setInscripcionesList((prev) => [...prev, { ...initialInscripcion, alumnoId }]);
  };

  // Función para eliminar un formulario de inscripción (por índice)
  const eliminarInscripcionRow = (index: number) => {
    setInscripcionesList((prev) => prev.filter((_, i) => i !== index));
  };

  // Función para "editar" una inscripción previa: la carga en el listado de formularios
  const handleEditarInscripcion = (ins: InscripcionResponse) => {
    console.log("Acá funciona 1");

    // Buscamos la disciplina en el catálogo usando el id que trae el API en el objeto disciplina
    const disciplinaEncontrada = disciplinas.find(d => d.id === ins.disciplina.id);
    if (!disciplinaEncontrada) {
      console.log("Acá no funciona 1");
      toast.error("La inscripción seleccionada no tiene disciplina asignada.");
      return;
    }
    console.log("Acá funciona 2");
    const formData: InscripcionFormData = {
      id: ins.id,
      alumnoId: ins.alumno.id, // <-- Aquí usamos alumnoId en lugar de ins.alumno.id
      inscripcion: {
        disciplinaId: disciplinaEncontrada.id,
        bonificacionId: ins.bonificacion?.id,
      },
      fechaInscripcion: ins.fechaInscripcion || new Date().toISOString().split("T")[0],
    };
    // Actualizamos la lista de inscripciones
    setInscripcionesList((prev) => {
      const idx = prev.findIndex((item) => item.id === formData.id);
      if (idx !== -1) {
        const nuevos = [...prev];
        nuevos[idx] = formData;
        return nuevos;
      }
      return [...prev, formData];
    });
  };

  // Handler para guardar una inscripción (si tiene id, actualizar; sino, crear)
  const handleGuardarInscripcion = async (
    values: InscripcionFormData,
    resetForm: () => void
  ) => {
    if (!values.alumnoId || !values.inscripcion.disciplinaId) {
      toast.error("Debes asignar un alumno y una disciplina.");
      return;
    }
    try {
      if (values.id) {
        await inscripcionesApi.actualizar(values.id, {
          alumnoId: values.alumnoId,
          disciplinaId: values.inscripcion.disciplinaId,
          bonificacionId: values.inscripcion.bonificacionId,
        });
        toast.success("Inscripción actualizada correctamente.");
      } else {
        await inscripcionesApi.crear(values);
        toast.success("Inscripción creada correctamente.");
      }
      resetForm();
      // Opcional: limpiar el formulario de edición si se desea
      // También recargamos la lista de inscripciones previas
      fetchPrevInscripciones();
    } catch (err) {
      toast.error("Error al guardar la inscripción.");
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Registrar Inscripciones</h1>

      {/* Listado de inscripciones previas con botón "Editar" */}
      {prevInscripciones.length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-2">Inscripciones Previas</h2>
          <table className="min-w-full border">
            <thead>
              <tr className="bg-gray-200">
                <th className="px-4 py-2 border">ID</th>
                <th className="px-4 py-2 border">Disciplina</th>
                <th className="px-4 py-2 border">Fecha Inscripción</th>
                <th className="px-4 py-2 border">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {prevInscripciones.map((ins) => (
                <tr key={ins.id}>
                  <td className="px-4 py-2 border">{ins.id}</td>
                  <td className="px-4 py-2 border">{ins.disciplina?.nombre || "N/A"}</td>
                  <td className="px-4 py-2 border">{ins.fechaInscripcion}</td>
                  <td className="px-4 py-2 border">
                    <Boton onClick={() => handleEditarInscripcion(ins)} className="page-button-secondary">
                      Editar
                    </Boton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Sección de formularios para agregar/editar inscripciones */}
      {inscripcionesList.map((inscripcion, index) => (
        <div key={index} className="border p-4 mb-4 rounded">
          <Formik
            initialValues={inscripcion}
            validationSchema={inscripcionEsquema}
            onSubmit={(values, { setSubmitting, resetForm }) => {
              handleGuardarInscripcion(values, resetForm).finally(() => {
                setSubmitting(false);
              });
            }}
            enableReinitialize
          >
            {({ isSubmitting, values, setFieldValue }) => {
              const selectedDiscipline = disciplinas.find(
                (d) => d.id === Number(values.inscripcion.disciplinaId)
              );
              const cuota = selectedDiscipline?.valorCuota ?? 0;

              const selectedBonification = bonificaciones.find(
                (b) => b.id === Number(values.inscripcion.bonificacionId)
              );
              const bonificacionValor = selectedBonification?.valorFijo ?? 0;

              const total = cuota - bonificacionValor;

              return (
                <Form className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Campo oculto para alumnoId */}
                  <Field type="hidden" name="alumnoId" />

                  {/* Seleccionar Disciplina */}
                  <div className="mb-4">
                    <label htmlFor="inscripcion.disciplinaId" className="auth-label">
                      Disciplina:
                    </label>
                    <Field
                      as="select"
                      name="inscripcion.disciplinaId"
                      className="form-input"
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                        setFieldValue("inscripcion.disciplinaId", Number(e.target.value));
                      }}
                    >
                      <option value={0}>-- Seleccionar --</option>
                      {disciplinas.map((disc) => (
                        <option key={disc.id} value={disc.id}>
                          {disc.nombre}
                        </option>
                      ))}
                    </Field>
                    <ErrorMessage name="inscripcion.disciplinaId" component="div" className="auth-error" />
                  </div>

                  {/* Mostrar Cuota (solo lectura) */}
                  <div className="mb-4">
                    <label className="auth-label">Cuota:</label>
                    <input
                      type="number"
                      value={cuota}
                      readOnly
                      className="form-input bg-gray-100"
                    />
                  </div>

                  {/* Seleccionar Bonificación */}
                  <div className="mb-4">
                    <label htmlFor="inscripcion.bonificacionId" className="auth-label">
                      Bonificación (Opcional):
                    </label>
                    <Field
                      as="select"
                      name="inscripcion.bonificacionId"
                      className="form-input"
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                        setFieldValue("inscripcion.bonificacionId", e.target.value ? Number(e.target.value) : undefined);
                      }}
                    >
                      <option value="">-- Ninguna --</option>
                      {bonificaciones.map((bon) => (
                        <option key={bon.id} value={bon.id}>
                          {bon.descripcion}
                        </option>
                      ))}
                    </Field>
                    <ErrorMessage name="inscripcion.bonificacionId" component="div" className="auth-error" />
                  </div>

                  {/* Mostrar Valor de Bonificación (solo lectura) */}
                  <div className="mb-4">
                    <label className="auth-label">Valor Bonificación:</label>
                    <input
                      type="number"
                      value={bonificacionValor}
                      readOnly
                      className="form-input bg-gray-100"
                    />
                  </div>

                  {/* Fecha de Inscripción */}
                  <div className="mb-4">
                    <label htmlFor="fechaInscripcion" className="auth-label">
                      Fecha de Inscripción:
                    </label>
                    <Field
                      name="fechaInscripcion"
                      type="date"
                      className="form-input"
                    />
                    <ErrorMessage name="fechaInscripcion" component="div" className="auth-error" />
                  </div>

                  {/* Mostrar Total Calculado */}
                  <div className="mb-4">
                    <label className="auth-label">Total (Cuota - Bonificación):</label>
                    <input
                      type="number"
                      value={total}
                      readOnly
                      className="form-input bg-gray-100"
                    />
                  </div>

                  {/* Botones para Guardar y Eliminar */}
                  <div className="mb-4 flex gap-2 col-span-full">
                    <Boton type="submit" disabled={isSubmitting} className="page-button">
                      {values.id ? "Actualizar" : "Guardar"} Inscripción
                    </Boton>
                    <Boton
                      type="button"
                      onClick={() => eliminarInscripcionRow(index)}
                      className="page-button-secondary"
                    >
                      Eliminar
                    </Boton>
                  </div>
                </Form>
              );
            }}
          </Formik>
        </div>
      ))}

      <div className="flex gap-4">
        <Boton onClick={agregarInscripcion} className="page-button">
          Agregar Inscripción
        </Boton>
        <Boton
          onClick={() =>
            navigate(
              alumnoId ? `/alumnos/formulario?id=${alumnoId}` : "/inscripciones"
            )
          }
          className="page-button-secondary"
        >
          Volver
        </Boton>
      </div>
    </div>
  );
};

export default InscripcionesFormulario;
