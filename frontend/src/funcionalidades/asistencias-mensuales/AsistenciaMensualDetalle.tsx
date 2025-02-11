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
import { AsistenciaDiaria, EstadoAsistencia, AsistenciaMensualDetalleRequest } from "../../types/types";

const AsistenciaMensualDetalle: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [asistenciaMensual, setAsistenciaMensual] = useState<AsistenciaMensualDetalleRequest | null>(null);
  const [asistenciasDiarias, setAsistenciasDiarias] = useState<AsistenciaDiaria[]>([]);
  const [diasClase, setDiasClase] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});

  const cargarAsistenciaMensual = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    try {
      const response = await api.get<AsistenciaMensualDetalleRequest>(`/api/asistencias-mensuales/${id}/detalle`);
      const data = response.data;
      setAsistenciaMensual(data);
      setAsistenciasDiarias(data.asistenciasDiarias);

      // Calcular días únicos a partir de los registros diarios
      const fechas = data.asistenciasDiarias.map(ad => ad.fecha);
      const uniqueFechas = Array.from(new Set(fechas)).sort();
      setDiasClase(uniqueFechas);

      // Inicializar observaciones
      const obs = data.asistenciasDiarias.reduce((acc, ad) => {
        acc[ad.alumnoId] = ad.observacion || "";
        return acc;
      }, {} as Record<number, string>);
      setObservaciones(obs);
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

  // Función para alternar la asistencia usando el valor actual de fecha
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
        fecha: registro.fecha,
        estado: nuevoEstado,
        alumnoId: registro.alumnoId,
        asistenciaMensualId: registro.asistenciaMensualId,
        observacion: registro.observacion || "",
      });
      toast.success("Asistencia actualizada");
    } catch (error) {
      toast.error("Error al actualizar la asistencia.");
      // Revertir cambio en caso de error
      setAsistenciasDiarias(prev =>
        prev.map(a => a.id === registro.id ? { ...a, estado: registro.estado } : a)
      );
    }
  };

  // Actualización de observaciones sin debounce (ya que en este caso optamos por no usar debounce)
  const handleObservacionChange = (alumnoId: number, obs: string) => {
    setObservaciones(prev => ({ ...prev, [alumnoId]: obs }));
    // Aquí podrías invocar un endpoint para actualizar la observación, incluyendo el valor de fecha si es necesario
  };

  if (loading) return <div className="text-center py-4">Cargando asistencia...</div>;
  if (error) return <div className="text-center py-4 text-red-500">{error}</div>;

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>Detalle de Asistencia Mensual - {asistenciaMensual?.disciplina}</CardTitle>
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
              {asistenciaMensual?.alumnos.map(alumno => (
                <TableRow key={alumno.id}>
                  <TableCell>{`${alumno.apellido}, ${alumno.nombre}`}</TableCell>
                  {diasClase.map(fecha => {
                    const ad = asistenciasDiarias.find(a => a.fecha === fecha && a.alumnoId === alumno.id);
                    return (
                      <TableCell key={fecha} className="text-center">
                        <Button
                          size="sm"
                          variant={ad?.estado === EstadoAsistencia.PRESENTE ? "default" : "outline"}
                          onClick={() => toggleAsistencia(alumno.id, fecha)}
                        >
                          {ad?.estado === EstadoAsistencia.PRESENTE ? <Check className="h-4 w-4" /> : <X className="h-4 w-4" />}
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
