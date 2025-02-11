"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import asistenciasApi from "../../api/asistenciasApi";
import { toast } from "react-toastify";
import api from "../../api/axiosConfig";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../../componentes/ui/card";
import { Button } from "../../componentes/ui/button";
import { Loader2, Check, X } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../componentes/ui/table";
import { Input } from "../../componentes/ui/input";
import { EstadoAsistencia } from "../../types/types";

interface Alumno {
  id: number;
  nombre: string;
  apellido: string;
}

interface AsistenciaDiaria {
  id: number;
  fecha: string;
  estado: EstadoAsistencia;
  alumnoId: number;
  asistenciaMensualId: number;
  observacion?: string;
}

interface AsistenciaMensualDetalle {
  id: number;
  disciplina: string;
  mes: number;
  anio: number;
  alumnos: Alumno[];
  asistenciasDiarias: AsistenciaDiaria[];
}

const AsistenciaDiariaFormulario = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [asistenciaMensual, setAsistenciaMensual] =
    useState<AsistenciaMensualDetalle | null>(null);
  const [asistenciasDiarias, setAsistenciasDiarias] = useState<
    AsistenciaDiaria[]
  >([]);
  const [diasClase, setDiasClase] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [observaciones, setObservaciones] = useState<Record<number, string>>(
    {}
  );

  /** 🔹 Cargar la asistencia mensual y los días de clase */
  const cargarAsistenciaMensual = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const response = await api.get<AsistenciaMensualDetalle>(
        `/api/asistencias-mensuales/${id}/detalle`
      );

      setAsistenciaMensual(response.data);
      setAsistenciasDiarias(response.data.asistenciasDiarias);

      // Obtener días de clase
      const diasResponse = await api.get<string[]>(
        `/api/asistencias-mensuales/dias-clase`,
        {
          params: {
            disciplinaId: response.data.id,
            mes: response.data.mes,
            anio: response.data.anio,
          },
        }
      );
      setDiasClase(diasResponse.data);
    } catch (error) {
      console.error("Error al obtener asistencia mensual:", error);
      setError("Error al cargar la asistencia.");
      toast.error("No se pudo cargar la asistencia.");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    cargarAsistenciaMensual();
  }, [cargarAsistenciaMensual]);

  /** 🔹 Alternar asistencia */
  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    if (!asistenciaMensual) return;

    const registro = asistenciasDiarias.find(a => a.alumnoId === alumnoId && a.fecha === fecha);
    if (!registro) return;

    const nuevoEstado = registro.estado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE;

    // Actualización optimista
    setAsistenciasDiarias(prev =>
      prev.map(a => a.id === registro.id ? { ...a, estado: nuevoEstado } : a)
    );

    try {
      await asistenciasApi.registrarAsistenciaDiaria({
        id: registro.id,
        fecha: registro.fecha,             // Valor actual de fecha
        estado: nuevoEstado,               // Utilizamos el nuevo estado calculado
        alumnoId: registro.alumnoId,         // Campo obligatorio
        asistenciaMensualId: registro.asistenciaMensualId, // Campo obligatorio
        observacion: registro.observacion || "",
      });
      toast.success("Asistencia actualizada");
    } catch (error) {
      toast.error("Error al actualizar la asistencia.");
      // Revertir el cambio en caso de error
      setAsistenciasDiarias(prev =>
        prev.map(a => a.id === registro.id ? { ...a, estado: registro.estado } : a)
      );
    }
  };


  const handleObservacionChange = (alumnoId: number, observacion: string) => {
    setObservaciones((prev) => ({ ...prev, [alumnoId]: observacion }));
  };

  if (loading) return <Loader2 className="h-8 w-8 animate-spin mx-auto my-8" />;
  if (error) return <p className="text-red-500 text-center">{error}</p>;

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>Asistencia - {asistenciaMensual?.disciplina}</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Alumno</TableHead>
                {diasClase.map((fecha) => (
                  <TableHead key={fecha} className="text-center">
                    {new Date(fecha).toLocaleDateString("es-ES", {
                      day: "numeric",
                      weekday: "short",
                    })}
                  </TableHead>
                ))}
                <TableHead>Observaciones</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {asistenciaMensual?.alumnos.map((alumno) => (
                <TableRow key={alumno.id}>
                  <TableCell>{`${alumno.apellido}, ${alumno.nombre}`}</TableCell>
                  {diasClase.map((fecha) => {
                    const asistencia = asistenciasDiarias.find(
                      (a) => a.fecha === fecha && a.alumnoId === alumno.id
                    );
                    return (
                      <TableCell key={fecha} className="text-center">
                        <Button
                          size="sm"
                          variant={
                            asistencia?.estado === EstadoAsistencia.PRESENTE
                              ? "default"
                              : "outline"
                          }
                          onClick={() => toggleAsistencia(alumno.id, fecha)}
                        >
                          {asistencia?.estado === EstadoAsistencia.PRESENTE ? (
                            <Check className="h-4 w-4" />
                          ) : (
                            <X className="h-4 w-4" />
                          )}
                        </Button>
                      </TableCell>
                    );
                  })}
                  <TableCell>
                    <Input
                      placeholder="Observaciones..."
                      value={observaciones[alumno.id] || ""}
                      onChange={(e) =>
                        handleObservacionChange(alumno.id, e.target.value)
                      }
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <div className="mt-6 flex justify-end space-x-4">
            <Button onClick={() => navigate("/asistencias-diarias")}>
              Volver
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AsistenciaDiariaFormulario;
