"use client";

import { useState, useEffect } from "react";
import Boton from "../../componentes/comunes/Boton";
import { Field, Form, Formik } from "formik";
import disciplinasApi from "../../api/disciplinasApi";
import profesoresApi from "../../api/profesoresApi";
import asistenciasApi from "../../api/asistenciasApi";
import { toast } from "react-toastify";
import type { AsistenciaMensualListadoResponse } from "../../types/types";

interface ProfesorDisciplina {
  id: string;
  nombre: string;
}

const AsistenciasMensualesPagina = () => {
  const [profesoresDisciplinas, setProfesoresDisciplinas] = useState<
    ProfesorDisciplina[]
  >([]);
  const [asistencias, setAsistencias] = useState<
    AsistenciaMensualListadoResponse[]
  >([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProfesoresDisciplinas = async () => {
      try {
        const profesores = await profesoresApi.listarProfesoresActivos();
        const disciplinas = await disciplinasApi.listarDisciplinas();

        const profesoresDisciplinas = profesores.flatMap((profesor) =>
          disciplinas.map((disciplina) => ({
            id: `${profesor.id}-${disciplina.id}`,
            nombre: `${profesor.nombre} ${profesor.apellido} - ${disciplina.nombre}`,
          }))
        );

        setProfesoresDisciplinas(profesoresDisciplinas);
      } catch (error) {
        console.error("Error al cargar profesores y disciplinas:", error);
        toast.error("Error al cargar profesores y disciplinas");
      }
    };

    fetchProfesoresDisciplinas();
  }, []);

  const fetchAsistencias = async (
    profesorId?: number,
    disciplinaId?: number,
    mes?: number,
    anio?: number
  ) => {
    setLoading(true);
    try {
      const response = await asistenciasApi.listarAsistenciasMensuales(
        profesorId,
        disciplinaId,
        mes,
        anio
      );
      setAsistencias(response);
    } catch (error) {
      console.error("Error al obtener asistencias mensuales:", error);
      toast.error("Error al obtener asistencias mensuales");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const currentDate = new Date();
    fetchAsistencias(
      undefined,
      undefined,
      currentDate.getMonth() + 1,
      currentDate.getFullYear()
    );
  }, []); // Added empty dependency array to fix the warning

  const handleSubmit = (values: {
    profesorDisciplina: string;
    mes: number;
    anio: number;
  }) => {
    const [profesorId, disciplinaId] = values.profesorDisciplina
      .split("-")
      .map(Number);
    fetchAsistencias(profesorId, disciplinaId, values.mes, values.anio);
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Asistencias Mensuales</h1>
      <p className="mb-4">
        Las asistencias mensuales se crean automáticamente para cada disciplina
        activa al inicio de cada mes.
      </p>
      <Formik
        initialValues={{
          profesorDisciplina: "",
          mes: new Date().getMonth() + 1,
          anio: new Date().getFullYear(),
        }}
        onSubmit={handleSubmit}
      >
        <Form className="mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="profesorDisciplina" className="block mb-2">
                Profesor y Disciplina:
              </label>
              <Field
                as="select"
                name="profesorDisciplina"
                className="form-input w-full"
              >
                <option value="">Todos</option>
                {profesoresDisciplinas.map((pd) => (
                  <option key={pd.id} value={pd.id}>
                    {pd.nombre}
                  </option>
                ))}
              </Field>
            </div>
            <div>
              <label htmlFor="mes" className="block mb-2">
                Mes:
              </label>
              <Field as="select" name="mes" className="form-input w-full">
                {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                  <option key={m} value={m}>
                    {new Date(2000, m - 1, 1).toLocaleString("default", {
                      month: "long",
                    })}
                  </option>
                ))}
              </Field>
            </div>
            <div>
              <label htmlFor="anio" className="block mb-2">
                Año:
              </label>
              <Field
                type="number"
                name="anio"
                className="form-input w-full"
                min={2000}
                max={2100}
              />
            </div>
          </div>
          <Boton type="submit" className="mt-4">
            Buscar Asistencias
          </Boton>
        </Form>
      </Formik>

      {loading ? (
        <div>Cargando...</div>
      ) : (
        <div>
          {asistencias.length > 0 ? (
            <table className="min-w-full bg-white">
              <thead>
                <tr>
                  <th className="py-2 px-4 border-b">Profesor</th>
                  <th className="py-2 px-4 border-b">Disciplina</th>
                  <th className="py-2 px-4 border-b">Mes</th>
                  <th className="py-2 px-4 border-b">Año</th>
                  <th className="py-2 px-4 border-b">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {asistencias.map((asistencia) => (
                  <tr key={asistencia.id}>
                    <td className="py-2 px-4 border-b">
                      {asistencia.profesor}
                    </td>
                    <td className="py-2 px-4 border-b">
                      {asistencia.disciplina}
                    </td>
                    <td className="py-2 px-4 border-b">{asistencia.mes}</td>
                    <td className="py-2 px-4 border-b">{asistencia.anio}</td>
                    <td className="py-2 px-4 border-b">
                      <Boton
                        onClick={() => {
                          // Implement navigation to detail view
                          console.log(
                            `Ver detalle de asistencia ${asistencia.id}`
                          );
                        }}
                      >
                        Ver Detalle
                      </Boton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div>
              No se encontraron asistencias para los criterios seleccionados.
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AsistenciasMensualesPagina;
