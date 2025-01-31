import React, { useEffect, useState, useCallback } from "react";
import { Form, useNavigate, useSearchParams } from "react-router-dom";
import { ErrorMessage, Field, Formik } from "formik";
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

  const initialValues: InscripcionRequest = {
    alumnoId: 0,
    disciplinaId: 0,
    bonificacionId: undefined,
    costoParticular: 0,
    notas: "",
  };

  // Cargar disciplinas y bonificaciones al montar el componente
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [discData, bonData] = await Promise.all([
          disciplinasApi.listarDisciplinas(),
          bonificacionesApi.listarBonificaciones(),
        ]);
        setDisciplinas(discData);
        setBonificaciones(bonData);
      } catch (error) {
        toast.error("Error al cargar disciplinas o bonificaciones.");
      }
    };
    fetchData();
  }, []);

  // Determinar si estamos en edición o creación
  useEffect(() => {
    const idParam = searchParams.get("id");
    const alumnoParam = searchParams.get("alumnoId");

    if (idParam) {
      cargarInscripcion(idParam);
    } else if (alumnoParam) {
      const aId = Number(alumnoParam);
      if (!isNaN(aId)) {
        initialValues.alumnoId = aId;
      }
    }
  }, [searchParams]);

  // Cargar una inscripción existente
  const cargarInscripcion = useCallback(async (idStr: string) => {
    try {
      const idNum = Number(idStr);
      if (isNaN(idNum)) {
        toast.error("ID de inscripción inválido");
        return;
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

  // Manejar guardado de inscripción
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

  // Manejar selección de bonificación y cálculo del costo final
  const handleSelectBonificacion = (
    value: string,
    values: InscripcionRequest,
    setValues: (values: InscripcionRequest) => void
  ) => {
    const bonificacionId = value === "" ? undefined : Number(value);
    let costoFinal = values.costoParticular ?? 0;

    if (bonificacionId) {
      const bonificacionSeleccionada = bonificaciones.find(
        (b) => b.id === bonificacionId
      );
      if (bonificacionSeleccionada) {
        costoFinal -=
          (costoFinal * bonificacionSeleccionada.porcentajeDescuento) / 100;
      }
    }

    setValues({ ...values, bonificacionId, costoParticular: costoFinal });
  };

  // Manejar limpieza del formulario
  const handleLimpiar = (setValues: (values: InscripcionRequest) => void) => {
    setValues(initialValues);
    setInscripcionId(null);
  };

  // Manejar retorno a la vista de inscripciones o ficha del alumno
  const handleVolver = (alumnoId?: number) => {
    navigate(
      alumnoId ? `/alumnos/formulario?id=${alumnoId}` : "/inscripciones"
    );
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
        {({ values, setValues, isSubmitting }) => (
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
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
                  handleSelectBonificacion(e.target.value, values, setValues)
                }
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
                onClick={() => handleLimpiar(setValues)}
              >
                Limpiar
              </Boton>
              <Boton
                type="button"
                secondary
                onClick={() => handleVolver(values.alumnoId)}
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
