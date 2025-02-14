"use client";

import { useState, useEffect } from "react";
import { toast } from "react-toastify";
import asistenciasApi from "../../api/asistenciasApi";
import type { AsistenciaMensualListadoResponse } from "../../types/types";

interface ResumenAsistenciasMensualesProps {
  mes: number;
  anio: number;
  onVerDetalle: (
    profesorDisciplinaId: string,
    mes: number,
    anio: number
  ) => void;
}

export default function ResumenAsistenciasMensuales({
  mes,
  anio,
  onVerDetalle,
}: ResumenAsistenciasMensualesProps) {
  const [asistencias, setAsistencias] = useState<
    AsistenciaMensualListadoResponse[]
  >([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const cargarAsistencias = async () => {
      try {
        const data = await asistenciasApi.listarAsistenciasMensuales(
          mes,
          anio
        );
        setAsistencias(data);
      } catch (error) {
        console.error("Error al cargar el listado de asistencias:", error);
        toast.error("Error al cargar el listado de asistencias");
      } finally {
        setLoading(false);
      }
    };

    cargarAsistencias();
  }, [mes, anio]);

  const handleVerDetalle = (profesorDisciplinaId: string) => {
    onVerDetalle(profesorDisciplinaId, mes, anio);
  };

  if (loading) return <div>Cargando...</div>;

  return (
    <div className="bg-white shadow-md rounded-lg w-full">
      <div className="px-6 py-4 border-b">
        <h2 className="text-xl font-semibold">
          Resumen de Asistencias -{" "}
          {new Date(anio, mes - 1).toLocaleDateString("es", {
            month: "long",
            year: "numeric",
          })}
        </h2>
      </div>
      <div className="p-6">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th className="border px-4 py-2">Profesor</th>
                <th className="border px-4 py-2">Disciplina</th>
                <th className="border px-4 py-2">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {asistencias.map((asistencia) => (
                <tr key={asistencia.id}>
                  <td className="border px-4 py-2">{asistencia.profesor}</td>
                  <td className="border px-4 py-2">{asistencia.disciplina}</td>
                  <td className="border px-4 py-2">
                    <button
                      onClick={() => handleVerDetalle(asistencia.id.toString())}
                      className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                    >
                      Ver Detalle
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
