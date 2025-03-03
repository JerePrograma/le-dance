// src/funcionalidades/asistencias-mensuales/AsistenciasMensualesFormulario.tsx
import React, { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage, FormikHelpers } from "formik";
import { asistenciaMensualEsquema } from "../../validaciones/asistenciaMensualEsquema";
import asistenciasApi from "../../api/asistenciasApi";
import Boton from "../../componentes/comunes/Boton";
import { toast } from "react-toastify";
import disciplinasApi from "../../api/disciplinasApi";
import type { AsistenciaMensualRegistroRequest } from "../../types/types";
import type { DisciplinaListadoResponse } from "../../types/types";

const initialAsistenciaMensualValues: AsistenciaMensualRegistroRequest = {
  mes: new Date().getMonth() + 1,
  anio: new Date().getFullYear(),
  inscripcionId: undefined,
};

const AsistenciasMensualesFormulario: React.FC = () => {
  const navigate = useNavigate();
  const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);

  // Carga dinamica de disciplinas mediante Axios
  const fetchDisciplinas = useCallback(async () => {
    try {
      const data = await disciplinasApi.listarDisciplinasSimplificadas();
      setDisciplinas(data);
    } catch (error) {
      console.error("Error al cargar disciplinas:", error);
      toast.error("Error al cargar la lista de disciplinas. Intente nuevamente.");
    }
  }, []);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  const handleGuardar = useCallback(
    async (
      values: AsistenciaMensualRegistroRequest,
      { setSubmitting }: FormikHelpers<AsistenciaMensualRegistroRequest>
    ) => {
      try {
        // Llama al endpoint de registro de asistencia mensual
        await asistenciasApi.registrarAsistenciaMensual(values);
        toast.success("Asistencia mensual registrada correctamente. Se creara a partir del 1ro de cada mes.");
        // Redirige al listado actualizado
        navigate("/asistencias-mensuales");
      } catch (error: any) {
        console.error("Error al guardar la asistencia mensual:", error);
        // Suponiendo que el backend lanza un error en caso de duplicado
        toast.error("Error: Ya existe una asistencia mensual para la disciplina y el periodo seleccionado.");
      } finally {
        setSubmitting(false);
      }
    },
    [navigate]
  );

  return (
    <div className="page-container">
      <h1 className="page-title">Formulario de Asistencia Mensual</h1>
      <p className="mb-4 text-center">
        La asistencia se creara a partir del 1ro de cada mes.
      </p>
      <Formik
        initialValues={initialAsistenciaMensualValues}
        validationSchema={asistenciaMensualEsquema}
        onSubmit={handleGuardar}
      >
        {({ isSubmitting, resetForm }) => (
          <Form className="formulario max-w-4xl mx-auto">
            <div className="form-grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Desplegable de Disciplina / Profesor */}
              <div className="mb-4">
                <label htmlFor="inscripcionId" className="auth-label">
                  Disciplina / Profesor:
                </label>
                <Field as="select" id="inscripcionId" name="inscripcionId" className="form-input">
                  <option value="">Seleccione una disciplina...</option>
                  {disciplinas.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.nombre} - Profesor: {d.profesorNombre}
                    </option>
                  ))}
                </Field>

                <ErrorMessage name="inscripcionId" component="div" className="auth-error" />
              </div>
              {/* Campo para Mes */}
              <div className="mb-4">
                <label htmlFor="mes" className="auth-label">
                  Mes:
                </label>
                <Field
                  type="number"
                  id="mes"
                  name="mes"
                  min="1"
                  max={new Date().getMonth() + 1} // Restringe al mes actual
                  className="form-input"
                />
                <ErrorMessage name="mes" component="div" className="auth-error" />
              </div>
              {/* Campo para Año */}
              <div className="mb-4">
                <label htmlFor="anio" className="auth-label">
                  Año:
                </label>
                <Field
                  type="number"
                  id="anio"
                  name="anio"
                  max={new Date().getFullYear()} // Restringe al año actual
                  className="form-input"
                />
                <ErrorMessage name="anio" component="div" className="auth-error" />
              </div>
            </div>
            <div className="form-acciones">
              <Boton type="submit" disabled={isSubmitting} className="page-button">
                Registrar Asistencia Mensual
              </Boton>
              <Boton type="button" onClick={() => resetForm()} className="page-button-secondary">
                Limpiar
              </Boton>
              <Boton type="button" onClick={() => navigate("/asistencias-mensuales")} className="page-button-secondary">
                Volver al Listado
              </Boton>
            </div>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default AsistenciasMensualesFormulario;
