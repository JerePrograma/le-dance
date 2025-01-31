import React, { useEffect, useState, useCallback } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import { inscripcionEsquema } from "../../validaciones/inscripcionEsquema";
import inscripcionesApi from "../../utilidades/inscripcionesApi";
import disciplinasApi from "../../utilidades/disciplinasApi";
import bonificacionesApi from "../../utilidades/bonificacionesApi";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";

import {
  InscripcionRequest,
  InscripcionResponse,
  DisciplinaResponse,
  BonificacionResponse,
} from "../../types/types";

const InscripcionesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [disciplinas, setDisciplinas] = useState<DisciplinaResponse[]>([]);
  const [bonificaciones, setBonificaciones] = useState<BonificacionResponse[]>(
    []
  );
  const [inscripcionId, setInscripcionId] = useState<number | null>(null);
  const [initialValues, setInitialValues] = useState<InscripcionRequest>({
    alumnoId: 0,
    disciplinaId: 0,
    bonificacionId: undefined,
    costoParticular: 0,
    notas: "",
  });

  // Cargar disciplinas y bonificaciones al montar el componente
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

  // Cargar una inscripción existente si hay un ID en la URL
  const cargarInscripcion = useCallback(async (idStr: string) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID de inscripción inválido");
        return initialValues;
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
      return initialValues;
    }
  }, []);

  useEffect(() => {
    const idParam = searchParams.get("id");
    const alumnoParam = searchParams.get("alumnoId");

    if (idParam) {
      cargarInscripcion(idParam).then((data) => {
        if (data) setInitialValues(data);
      });
    } else if (alumnoParam) {
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        setInitialValues((prev) => ({ ...prev, alumnoId: aId }));
      }
    }
  }, [searchParams, cargarInscripcion]);

  const handleGuardar = async (values: InscripcionRequest) => {
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
  };

  return (
    <div className="formulario">
      <h1 className="form-title">
        {inscripcionId ? "Editar Inscripción" : "Nueva Inscripción"}
      </h1>

      <Formik
        initialValues={initialValues}
        validationSchema={inscripcionEsquema}
        onSubmit={handleGuardar}
        enableReinitialize
      >
        {({ values, setFieldValue, isSubmitting }) => (
          <Form className="formulario">
            {/* Alumno ID */}
            <div className="form-grid">
              <label>Alumno ID:</label>
              <Field
                name="alumnoId"
                type="number"
                className="form-input"
                disabled={!!inscripcionId}
              />
              <ErrorMessage name="alumnoId" component="div" className="error" />
            </div>

            {/* Disciplina */}
            <div className="form-grid">
              <label>Disciplina:</label>
              <Field as="select" name="disciplinaId" className="form-input">
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
                className="error"
              />
            </div>

            {/* Bonificación */}
            <div className="form-grid">
              <label>Bonificación:</label>
              <Field
                as="select"
                name="bonificacionId"
                className="form-input"
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => {
                  const value =
                    e.target.value === "" ? undefined : Number(e.target.value);
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
                className="error"
              />
            </div>

            {/* Costo Particular */}
            <div className="form-grid">
              <label>Costo Particular:</label>
              <Field
                name="costoParticular"
                type="number"
                className="form-input"
              />
              <ErrorMessage
                name="costoParticular"
                component="div"
                className="error"
              />
            </div>

            {/* Notas */}
            <div className="form-grid">
              <label>Notas:</label>
              <Field as="textarea" name="notas" className="form-input" />
              <ErrorMessage name="notas" component="div" className="error" />
            </div>

            {/* Botones de acción */}
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting}>
                {inscripcionId ? "Actualizar" : "Guardar"} Inscripción
              </Boton>
              <Boton
                type="reset"
                secondary
                onClick={() =>
                  setFieldValue("alumnoId", initialValues.alumnoId)
                }
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                secondary
                onClick={() =>
                  navigate(
                    values.alumnoId
                      ? `/alumnos/formulario?id=${values.alumnoId}`
                      : "/inscripciones"
                  )
                }
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
