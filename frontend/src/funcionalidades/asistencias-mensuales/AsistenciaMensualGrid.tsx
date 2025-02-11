"use client";

import { useCallback, useState } from "react";
import { toast } from "react-toastify";
import {
  type AsistenciaMensualDetalleResponse,
  type AsistenciaDiariaRegistroRequest,
  EstadoAsistencia,
} from "../../types/types";
import asistenciasApi from "../../api/asistenciasApi";

interface AsistenciaMensualGridProps {
  asistencia: AsistenciaMensualDetalleResponse;
}

export default function AsistenciaMensualGrid({
  asistencia,
}: AsistenciaMensualGridProps) {
  // ✅ Inicializar observaciones como un objeto `Record<number, string>` basado en `asistencia.observaciones`
  const [observaciones, setObservaciones] = useState<Record<number, string>>(
    asistencia.observaciones.reduce((acc, obs) => {
      acc[obs.alumnoId] = obs.observacion;
      return acc;
    }, {} as Record<number, string>)
  );

  const getSabadosDelMes = (mes: number, anio: number): Date[] => {
    const sabados: Date[] = [];
    const fecha = new Date(anio, mes - 1, 1);

    while (fecha.getMonth() === mes - 1) {
      if (fecha.getDay() === 6) {
        sabados.push(new Date(fecha));
      }
      fecha.setDate(fecha.getDate() + 1);
    }

    return sabados;
  };

  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    try {
      const asistenciaDiaria: AsistenciaDiariaRegistroRequest = {
        asistenciaMensualId: asistencia.id,
        alumnoId,
        fecha,
        estado: EstadoAsistencia.PRESENTE,
      };

      await asistenciasApi.registrarAsistenciaDiaria(asistenciaDiaria);

      toast.success("Asistencia actualizada");
    } catch (error) {
      console.error("Error al registrar asistencia:", error);
      toast.error("Error al registrar la asistencia");
    }
  };

  // Función debounce sencilla (sin dependencias externas)
  function debounce<T extends (...args: any[]) => void>(fn: T, delay: number): T {
    let timer: ReturnType<typeof setTimeout>;
    return function (...args: any[]) {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => fn(...args), delay);
    } as T;
  }

  const debouncedActualizarObservacion = useCallback(
    debounce(async (alumnoId: number, obs: string) => {
      try {
        await asistenciasApi.actualizarAsistenciaMensual(asistencia.id, {
          observaciones: Object.entries({
            ...observaciones,
            [alumnoId]: obs,
          }).map(([id, observacion]) => ({
            alumnoId: Number(id),
            observacion,
          })),
        });
        toast.success("Observación actualizada");
      } catch (error) {
        console.error("Error al guardar observación:", error);
        toast.error("Error al guardar la observación");
      }
    }, 500),
    [asistencia, observaciones]
  );

  const handleObservacionChange = (alumnoId: number, obs: string) => {
    setObservaciones((prev) => ({ ...prev, [alumnoId]: obs }));
    debouncedActualizarObservacion(alumnoId, obs);
  };

  const sabados = getSabadosDelMes(asistencia.mes, asistencia.anio);

  return (
    <div className="bg-white shadow-md rounded-lg w-full">
      <div className="px-6 py-4 border-b">
        <h2 className="text-xl font-semibold flex items-center justify-between">
          <span>{asistencia.disciplina}</span>
          <span className="text-sm font-normal">
            {new Date(asistencia.anio, asistencia.mes - 1).toLocaleDateString(
              "es",
              {
                month: "long",
                year: "numeric",
              }
            )}
          </span>
        </h2>
      </div>
      <div className="p-6">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse">
            <thead>
              <tr>
                <th className="border px-4 py-2">Nombre</th>
                {sabados.map((sabado) => (
                  <th key={sabado.toISOString()} className="border px-4 py-2">
                    {sabado.getDate()}
                  </th>
                ))}
                <th className="border px-4 py-2">Observaciones</th>
              </tr>
            </thead>
            <tbody>
              {asistencia.alumnos.map((alumno) => (
                <tr key={alumno.id}>
                  <td className="border px-4 py-2">{`${alumno.apellido}, ${alumno.nombre}`}</td>
                  {sabados.map((sabado) => {
                    const fecha = sabado.toISOString().split("T")[0];
                    const asistenciaDiaria = asistencia.asistenciasDiarias.find(
                      (ad) => ad.alumnoId === alumno.id && ad.fecha === fecha
                    );

                    return (
                      <td key={fecha} className="border px-4 py-2 text-center">
                        <input
                          type="checkbox"
                          checked={
                            asistenciaDiaria?.estado ===
                            EstadoAsistencia.PRESENTE
                          }
                          onChange={() => toggleAsistencia(alumno.id, fecha)}
                          className="form-checkbox h-5 w-5 text-primary"
                        />
                      </td>
                    );
                  })}
                  <td className="border px-4 py-2">
                    <input
                      type="text"
                      placeholder="Observaciones..."
                      className="w-full px-2 py-1 border rounded"
                      value={observaciones[alumno.id] || ""}
                      onChange={(e) =>
                        handleObservacionChange(alumno.id, e.target.value)
                      }
                    />
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
