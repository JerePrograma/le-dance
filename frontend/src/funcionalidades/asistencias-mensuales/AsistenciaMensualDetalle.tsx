// src/funcionalidades/asistencias-mensuales/AsistenciaMensualDetalle.tsx
import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import api from "../../api/axiosConfig";
import { Check, X } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../componentes/ui/card";
import { Button } from "../../componentes/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../componentes/ui/table";
import { Input } from "../../componentes/ui/input";
import asistenciasApi from "../../api/asistenciasApi";
import { AsistenciaMensualDetalleResponse, EstadoAsistencia } from "../../types/types";

const AsistenciaMensualDetalle: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [asistencia, setAsistencia] = useState<AsistenciaMensualDetalleResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});

  // Función de debounce sencilla sin dependencias adicionales
  const debounce = (func: (...args: any[]) => void, delay: number) => {
    let timer: NodeJS.Timeout;
    return (...args: any[]) => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        func(...args);
      }, delay);
    };
  };

  const cargarAsistencia = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const response = await api.get<AsistenciaMensualDetalleResponse>(`/api/asistencias-mensuales/${id}/detalle`);
      const data = response.data;
      setAsistencia(data);
      // Inicializar observaciones a partir de las observaciones guardadas
      const obs = data.observaciones.reduce((acc, o) => {
        acc[o.alumnoId] = o.observacion;
        return acc;
      }, {} as Record<number, string>);
      setObservaciones(obs);
    } catch (error) {
      console.error("Error al cargar asistencia:", error);
      setError("Error al cargar la asistencia.");
      toast.error("No se pudo cargar la asistencia.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    cargarAsistencia();
  }, [cargarAsistencia]);

  // Función para alternar el estado de una asistencia diaria
  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    if (!asistencia) return;
    const registro = asistencia.asistenciasDiarias.find(a => a.alumnoId === alumnoId && a.fecha === fecha);
    if (!registro) return;
    // Normalizamos el valor a mayúsculas para la comparación
    const currentEstado = String(registro.estado).toUpperCase();
    const nuevoEstado = currentEstado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE;

    // Actualización optimista
    setAsistencia(prev => {
      if (!prev) return prev;
      const nuevasAsistencias = prev.asistenciasDiarias.map(a =>
        a.id === registro.id ? { ...a, estado: nuevoEstado } : a
      );
      return { ...prev, asistenciasDiarias: nuevasAsistencias };
    });

    try {
      await asistenciasApi.registrarAsistenciaDiaria({
        id: registro.id,
        fecha: registro.fecha, // La fecha es inmutable
        estado: nuevoEstado,
        alumnoId: registro.alumnoId,
        asistenciaMensualId: registro.asistenciaMensualId,
        observacion: registro.observacion || "",
      });
      toast.success("Asistencia actualizada");
    } catch (error) {
      toast.error("Error al actualizar la asistencia.");
      // Revertir el cambio en caso de error
      setAsistencia(prev => {
        if (!prev) return prev;
        const revertidas = prev.asistenciasDiarias.map(a =>
          a.id === registro.id ? { ...a, estado: registro.estado } : a
        );
        return { ...prev, asistenciasDiarias: revertidas };
      });
    }
  };

  // Actualización de observaciones con debounce
  const debouncedActualizarObservacion = useCallback(
    debounce(async (alumnoId: number, obs: string) => {
      if (!asistencia) return;
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
    setObservaciones(prev => ({ ...prev, [alumnoId]: obs }));
    debouncedActualizarObservacion(alumnoId, obs);
  };

  if (loading) return <div className="text-center py-4">Cargando asistencia...</div>;
  if (error) return <div className="text-center py-4 text-red-500">{error}</div>;
  if (!asistencia) return <div className="text-center py-4">No se encontró asistencia.</div>;

  // Calcular días únicos a partir de los registros diarios
  const diasClase = Array.from(new Set(asistencia.asistenciasDiarias.map(a => a.fecha))).sort();

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>Detalle de Asistencia Mensual - {asistencia.disciplina}</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Alumno</TableHead>
                {diasClase.map(fecha => (
                  <TableHead key={fecha} className="text-center">
                    {new Date(fecha).toLocaleDateString("es-ES", { day: "numeric", weekday: "short" })}
                  </TableHead>
                ))}
                <TableHead>Observaciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {asistencia.alumnos.map(alumno => (
                <TableRow key={alumno.id}>
                  <TableCell>{`${alumno.apellido}, ${alumno.nombre}`}</TableCell>
                  {diasClase.map(fecha => {
                    const registro = asistencia.asistenciasDiarias.find(a => a.alumnoId === alumno.id && a.fecha === fecha);
                    return (
                      <TableCell key={fecha} className="text-center">
                        <Button
                          size="sm"
                          variant={registro?.estado === EstadoAsistencia.PRESENTE ? "default" : "outline"}
                          onClick={() => toggleAsistencia(alumno.id, fecha)}
                        >
                          {registro?.estado === EstadoAsistencia.PRESENTE ? <Check className="h-4 w-4" /> : <X className="h-4 w-4" />}
                        </Button>
                      </TableCell>
                    );
                  })}
                  <TableCell>
                    <Input
                      placeholder="Observaciones..."
                      value={observaciones[alumno.id] || ""}
                      onChange={(e) => handleObservacionChange(alumno.id, e.target.value)}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <div className="mt-6 flex justify-end space-x-4">
            <Button onClick={() => navigate("/asistencias-mensuales")}>Volver</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AsistenciaMensualDetalle;
