"use client";

import { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { toast } from "react-toastify";
import Boton from "../../componentes/comunes/Boton";
import { Search } from "lucide-react";

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

// Esquema de validacion
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";

// Valores iniciales (sin fechaBaja)
const initialInscripcionValues: InscripcionRegistroRequest = {
  alumnoId: 0,
  inscripcion: {
    disciplinaId: 0,
    bonificacionId: undefined,
  },
  fechaInscripcion: new Date().toISOString().split("T")[0], // p.ej. "2025-02-06"
  notas: "",
};

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  // Listados
  const [disciplinas, setDisciplinas] = useState<DisciplinaDetalleResponse[]>(
    []
  );
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );

  // ID de inscripcion para saber si estamos en modo "edicion" o "nuevo"
  const [inscripcionId, setInscripcionId] = useState<number | null>(null);

  // Estado con los valores del formulario
  const [initialValues, setInitialValues] =
    useState<InscripcionRegistroRequest>(initialInscripcionValues);

  // Cargar catalogos (Disciplinas, Bonificaciones) con Promise.all
  useEffect(() => {
    const fetchData = async () => {
      try {
        // 👇 Ajuste con casting de tuplas
        const [discData, bonData] = (await Promise.all([
          disciplinasApi.listarDisciplinas(), // Retorna DisciplinaDetalleResponse[]
          bonificacionesApi.listarBonificaciones(), // Retorna BonificacionResponse[]
        ])) as [DisciplinaDetalleResponse[], BonificacionResponse[]];

        setDisciplinas(discData || []);
        setBonificaciones(bonData || []);
      } catch (error) {
        toast.error("Error al cargar disciplinas o bonificaciones.");
      }
    };
    fetchData();
  }, []);

  // Funcion para cargar datos de una Inscripcion existente
  const cargarInscripcion = useCallback(
    async (idStr: string): Promise<InscripcionRegistroRequest> => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID de inscripcion invalido");
          return initialInscripcionValues;
        }

        const data: InscripcionResponse = await inscripcionesApi.obtenerPorId(
          idNum
        );
        setInscripcionId(data.id);

        toast.success("Inscripcion cargada correctamente.");

        // Mapeo la respuesta (InscripcionResponse) a InscripcionRegistroRequest
        return {
          alumnoId: data.alumno.id,
          inscripcion: {
            disciplinaId: data.disciplina.id,
            bonificacionId: data.bonificacion?.id,
          },
          // Tomamos la fecha de inscripcion de la entidad si la tuvieras en tu backend
          // Aqui, para ejemplo, uso la de "data". Ajusta segun tu real response
          fechaInscripcion: new Date().toISOString().split("T")[0],
          notas: data.notas ?? "",
        };
      } catch (err) {
        toast.error("No se encontro la inscripcion con ese ID.");
        return initialInscripcionValues;
      }
    },
    []
  );

  // Efecto para leer parametros de la URL e inicializar el formulario
  useEffect(() => {
    const idParam = searchParams.get("id");
    const alumnoParam = searchParams.get("alumnoId");

    if (idParam) {
      // Modo edicion
      cargarInscripcion(idParam).then((data) => setInitialValues(data));
    } else if (alumnoParam) {
      // Modo nuevo con un alumno ya conocido
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        setInitialValues((prev) => ({ ...prev, alumnoId: aId }));
      }
    }
  }, [searchParams, cargarInscripcion]);

  // Handler para guardar/actualizar la inscripcion
  const handleGuardar = useCallback(
    async (values: InscripcionRegistroRequest) => {
      if (!values.alumnoId || !values.inscripcion.disciplinaId) {
        toast.error("Debes asignar un alumno y una disciplina.");
        return;
      }
      try {
        if (inscripcionId) {
          // Actualizar
          await inscripcionesApi.actualizar(inscripcionId, {
            alumnoId: values.alumnoId,
            disciplinaId: values.inscripcion.disciplinaId,
            bonificacionId: values.inscripcion.bonificacionId,
            // podrias añadir "fechaBaja", "activo", etc. si es que existen
            notas: values.notas,
          });
          toast.success("Inscripcion actualizada correctamente.");
        } else {
          // Crear nueva
          const newIns = await inscripcionesApi.crear(values);
          setInscripcionId(newIns.id);
          toast.success("Inscripcion creada correctamente.");
        }
      } catch (err) {
        toast.error("Error al guardar la inscripcion.");
      }
    },
    [inscripcionId]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">
        {inscripcionId ? "Editar Inscripcion" : "Nueva Inscripcion"}
      </h1>

      <Formik
        initialValues={initialValues}
        validationSchema={inscripcionEsquema}
        onSubmit={handleGuardar}
        enableReinitialize
      >
        {({ isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* BUSCADOR DE INSCRIPCIÓN */}
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Numero de Inscripcion:
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    id="idBusqueda"
                    className="form-input flex-grow"
                    onChange={(e) => {
                      const id = e.target.value;
                      cargarInscripcion(id).then((data) =>
                        setInitialValues(data)
                      );
                    }}
                  />
                  <Boton
                    onClick={() =>
                      cargarInscripcion(inscripcionId?.toString() || "").then(
                        (data) => setInitialValues(data)
                      )
                    }
                    className="page-button"
                  >
                    <Search className="w-5 h-5 mr-2" />
                    Buscar
                  </Boton>
                </div>
              </div>

              {/* ALUMNO */}
              <div className="mb-4">
                <label htmlFor="alumnoId" className="auth-label">
                  Alumno ID:
                </label>
                <Field
                  name="alumnoId"
                  type="number"
                  id="alumnoId"
                  className="form-input"
                  disabled={!!inscripcionId}
                />
                <ErrorMessage
                  name="alumnoId"
                  component="div"
                  className="auth-error"
                />
              </div>

              {/* DISCIPLINA */}
              <div className="mb-4">
                <label
                  htmlFor="inscripcion.disciplinaId"
                  className="auth-label"
                >
                  Disciplina:
                </label>
                <Field
                  as="select"
                  name="inscripcion.disciplinaId"
                  id="inscripcion.disciplinaId"
                  className="form-input"
                >
                  <option value={0}>-- Seleccionar --</option>
                  {disciplinas.map((disc) => (
                    <option key={disc.id} value={disc.id}>
                      {disc.id} - {disc.nombre}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="inscripcion.disciplinaId"
                  component="div"
                  className="auth-error"
                />
              </div>

              {/* BONIFICACIÓN */}
              <div className="mb-4">
                <label
                  htmlFor="inscripcion.bonificacionId"
                  className="auth-label"
                >
                  Bonificacion (Opcional):
                </label>
                <Field
                  as="select"
                  name="inscripcion.bonificacionId"
                  id="inscripcion.bonificacionId"
                  className="form-input"
                >
                  <option value="">-- Ninguna --</option>
                  {bonificaciones.map((bon) => (
                    <option key={bon.id} value={bon.id}>
                      {bon.descripcion}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="inscripcion.bonificacionId"
                  component="div"
                  className="auth-error"
                />
              </div>

              {/* FECHA INSCRIPCION */}
              <div className="mb-4">
                <label htmlFor="fechaInscripcion" className="auth-label">
                  Fecha de Inscripcion:
                </label>
                <Field
                  name="fechaInscripcion"
                  type="date"
                  id="fechaInscripcion"
                  className="form-input"
                />
                <ErrorMessage
                  name="fechaInscripcion"
                  component="div"
                  className="auth-error"
                />
              </div>
              {/* FECHA DE BAJA solo si existe ID */}
              {inscripcionId && (
                <div className="mb-4">
                  <label htmlFor="fechaBaja" className="auth-label">
                    Fecha de Baja:
                  </label>
                  <Field
                    name="fechaBaja"
                    type="date"
                    id="fechaBaja"
                    className="form-input"
                  />
                  <ErrorMessage
                    name="fechaBaja"
                    component="div"
                    className="auth-error"
                  />
                </div>
              )}

              {/* ... cualquier otro campo condicional ... */}

              {/* NOTAS */}
              <div className="col-span-full mb-4">
                <label htmlFor="notas" className="auth-label">
                  Notas:
                </label>
                <Field
                  as="textarea"
                  name="notas"
                  id="notas"
                  className="form-input h-24"
                />
                <ErrorMessage
                  name="notas"
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
                {inscripcionId ? "Actualizar" : "Guardar"} Inscripcion
              </Boton>
              <Boton
                type="reset"
                onClick={() => setInitialValues(initialInscripcionValues)}
                className="page-button-secondary"
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                onClick={() =>
                  navigate(
                    values.alumnoId
                      ? `/alumnos/formulario?id=${values.alumnoId}`
                      : "/inscripciones"
                  )
                }
                className="page-button-secondary"
              >
                Volver
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default InscripcionesFormulario;
