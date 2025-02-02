import type React from "react";
import { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Boton from "../../componentes/comunes/Boton";
import { ErrorMessage, Field, Form, Formik } from "formik";
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import disciplinasApi from "../../utilidades/disciplinasApi";
import bonificacionesApi from "../../utilidades/bonificacionesApi";
import { toast } from "react-toastify";
import type {
  InscripcionRequest,
  InscripcionResponse,
  DisciplinaResponse,
  BonificacionResponse,
} from "../../types/types";
import { Search } from "lucide-react";

const initialInscripcionValues: InscripcionRequest = {
  alumnoId: 0,
  disciplinaId: 0,
  bonificacionId: undefined,
  costoParticular: 0,
  notas: "",
};

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );
  const [inscripcionId, setInscripcionId] = useState<number | null>(null);
  const [initialValues, setInitialValues] = useState<InscripcionRequest>(
    initialInscripcionValues
  );

  useEffect(() => {
    const fetchData = async () => {
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
    fetchData();
  }, []);

  const cargarInscripcion = useCallback(
    async (idStr: string): Promise<InscripcionRequest> => {
      try {
        const idNum = Number(idStr);
        if (isNaN(idNum)) {
          toast.error("ID de inscripción inválido");
          return initialInscripcionValues;
        }
        const data: InscripcionResponse =
          await inscripcionesApi.obtenerInscripcionPorId(idNum);
        setInscripcionId(data.id);
        toast.success("Inscripción cargada correctamente.");
        return {
          alumnoId: data.alumnoId,
          disciplinaId: data.disciplinaId,
          bonificacionId: data.bonificacionId,
          costoParticular: data.costoParticular ?? 0,
          notas: data.notas ?? "",
        };
      } catch (err) {
        toast.error("No se encontró la inscripción con ese ID.");
        return initialInscripcionValues;
      }
    },
    []
  );

  useEffect(() => {
    const idParam = searchParams.get("id");
    const alumnoParam = searchParams.get("alumnoId");
    if (idParam) {
      cargarInscripcion(idParam).then((data) => {
        setInitialValues(data);
      });
    } else if (alumnoParam) {
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        setInitialValues((prev) => ({ ...prev, alumnoId: aId }));
      }
    }
  }, [searchParams, cargarInscripcion]);

  const handleGuardar = useCallback(
    async (values: InscripcionRequest) => {
      if (!values.alumnoId || !values.disciplinaId) {
        toast.error("Debes asignar un alumno y una disciplina.");
        return;
      }
      try {
        if (inscripcionId) {
          await inscripcionesApi.actualizarInscripcion(inscripcionId, values);
          toast.success("Inscripción actualizada correctamente.");
        } else {
          const newIns = await inscripcionesApi.crearInscripcion(values);
          setInscripcionId(newIns.id);
          toast.success("Inscripción creada correctamente.");
        }
      } catch (err) {
        toast.error("Error al guardar la inscripción.");
      }
    },
    [inscripcionId]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">
        {inscripcionId ? "Editar Inscripción" : "Nueva Inscripción"}
      </h1>
      <Formik
        initialValues={initialValues}
        validationSchema={inscripcionEsquema}
        onSubmit={handleGuardar}
        enableReinitialize
      >
        {({ setFieldValue, isSubmitting, values }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="col-span-full mb-4">
                <label htmlFor="idBusqueda" className="auth-label">
                  Número de Inscripción:
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

              <div className="mb-4">
                <label htmlFor="disciplinaId" className="auth-label">
                  Disciplina:
                </label>
                <Field
                  as="select"
                  name="disciplinaId"
                  id="disciplinaId"
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
                  name="disciplinaId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="bonificacionId" className="auth-label">
                  Bonificación:
                </label>
                <Field
                  as="select"
                  name="bonificacionId"
                  id="bonificacionId"
                  className="form-input"
                  onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                    const value =
                      e.target.value === ""
                        ? undefined
                        : Number(e.target.value);
                    const bonificacionSeleccionada = bonificaciones.find(
                      (b) => b.id === value
                    );
                    const descuento = bonificacionSeleccionada
                      ? bonificacionSeleccionada.porcentajeDescuento
                      : 0;
                    setFieldValue("bonificacionId", value);
                    setFieldValue(
                      "costoParticular",
                      values.costoParticular
                        ? values.costoParticular -
                            (values.costoParticular * descuento) / 100
                        : 0
                    );
                  }}
                >
                  <option value="">-- Ninguna --</option>
                  {bonificaciones.map((bon) => (
                    <option key={bon.id} value={bon.id}>
                      {bon.descripcion}
                    </option>
                  ))}
                </Field>
                <ErrorMessage
                  name="bonificacionId"
                  component="div"
                  className="auth-error"
                />
              </div>

              <div className="mb-4">
                <label htmlFor="costoParticular" className="auth-label">
                  Costo Particular:
                </label>
                <Field
                  name="costoParticular"
                  type="number"
                  id="costoParticular"
                  className="form-input"
                />
                <ErrorMessage
                  name="costoParticular"
                  component="div"
                  className="auth-error"
                />
              </div>

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
                {inscripcionId ? "Actualizar" : "Guardar"} Inscripción
              </Boton>
              <Boton
                type="reset"
                onClick={() =>
                  setFieldValue("alumnoId", initialInscripcionValues.alumnoId)
                }
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
