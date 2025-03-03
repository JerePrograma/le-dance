// src/funcionalidades/asistencias-mensuales/AsistenciaMensualDetalle.tsx
import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { Check, X } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../componentes/ui/card";
import { Button } from "../../componentes/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../componentes/ui/table";
import { Input } from "../../componentes/ui/input";
import asistenciasApi from "../../api/asistenciasApi";
import {
  AsistenciaMensualDetalleResponse,
  EstadoAsistencia,
  AsistenciaDiariaRegistroRequest,
} from "../../types/types";
import type { DisciplinaListadoResponse } from "../../types/types";

const AsistenciaMensualDetalle: React.FC = () => {
  const navigate = useNavigate();

  // Estados para filtros
  const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
  const [disciplineFilter, setDisciplineFilter] = useState<string>("");
  const [selectedDisciplineId, setSelectedDisciplineId] = useState<number | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<number | null>(null);
  const [selectedYear, setSelectedYear] = useState<number | null>(null);

  // Estado para el detalle de asistencia mensual
  const [asistenciaMensual, setAsistenciaMensual] = useState<AsistenciaMensualDetalleResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});

  // Funcion debounce (sin librerias externas)
  const debounce = (func: (...args: any[]) => void, delay: number) => {
    let timer: NodeJS.Timeout;
    return (...args: any[]) => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => func(...args), delay);
    };
  };

  // Cargar la lista de disciplinas (desde el endpoint de listado)
  const fetchDisciplinas = useCallback(async () => {
    try {
      const data = await asistenciasApi.listarDisciplinasSimplificadas();
      setDisciplinas(data);
      // Opcional: Si no hay disciplina seleccionada, se puede preseleccionar la primera
      if (data.length > 0 && !selectedDisciplineId) {
        setSelectedDisciplineId(data[0].id);
      }
    } catch (err) {
      console.error("Error al cargar disciplinas", err);
      toast.error("Error al cargar la lista de disciplinas.");
    }
  }, [selectedDisciplineId]);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  // Filtrar disciplinas segun el input de busqueda
  const filteredDisciplinas = useMemo(() => {
    if (!disciplineFilter.trim()) return disciplinas;
    return disciplinas.filter((d) =>
      d.nombre.toLowerCase().includes(disciplineFilter.toLowerCase())
    );
  }, [disciplineFilter, disciplinas]);

  // Funcion para consultar la asistencia mensual usando los filtros (GET)
  const cargarAsistenciaDinamica = useCallback(async () => {
    if (!selectedDisciplineId || !selectedMonth || !selectedYear) {
      toast.warn("Por favor, complete todos los filtros antes de consultar.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      // Llamamos al endpoint GET que obtiene la asistencia mensual por parametros
      const data = await asistenciasApi.obtenerAsistenciaMensualDetallePorParametros(
        selectedDisciplineId,
        selectedMonth,
        selectedYear
      );
      if (data) {
        setAsistenciaMensual(data);
        const obs = data.observaciones.reduce((acc, o) => {
          acc[o.alumnoId] = o.observacion;
          return acc;
        }, {} as Record<number, string>);
        setObservaciones(obs);
      } else {
        setAsistenciaMensual(null);
        toast.info("No se encontro asistencia mensual para estos parametros.");
      }
    } catch (err) {
      console.error("Error al cargar la asistencia mensual", err);
      setError("Error al cargar la asistencia mensual.");
      toast.error("No se pudo cargar la asistencia mensual.");
    } finally {
      setLoading(false);
    }
  }, [selectedDisciplineId, selectedMonth, selectedYear]);

  // Actualizar observaciones con debounce
  const debouncedActualizarObservacion = useCallback(
    debounce(async (alumnoId: number, obs: string) => {
      if (!asistenciaMensual) return;
      try {
        await asistenciasApi.actualizarAsistenciaMensual(asistenciaMensual.id, {
          observaciones: Object.entries({ ...observaciones, [alumnoId]: obs }).map(
            ([id, observacion]) => ({
              alumnoId: Number(id),
              observacion,
            })
          ),
        });
        toast.success("Observacion actualizada");
      } catch (err) {
        console.error("Error al actualizar observacion", err);
        toast.error("Error al actualizar la observacion.");
      }
    }, 500),
    [asistenciaMensual, observaciones]
  );

  const handleObservacionChange = (alumnoId: number, obs: string) => {
    setObservaciones(prev => ({ ...prev, [alumnoId]: obs }));
    debouncedActualizarObservacion(alumnoId, obs);
  };

  // Funcion para alternar la asistencia diaria de un alumno
  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    if (!asistenciaMensual) return;
    const registro = asistenciaMensual.asistenciasDiarias.find(
      ad => ad.alumnoId === alumnoId && ad.fecha === fecha
    );
    if (!registro) {
      toast.error("No se encontro registro de asistencia para este alumno en esta fecha.");
      return;
    }
    try {
      const nuevoEstado =
        registro.estado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE;
      const req: AsistenciaDiariaRegistroRequest = {
        id: registro.id,
        asistenciaMensualId: asistenciaMensual.id,
        alumnoId,
        fecha,
        estado: nuevoEstado,
      };
      await asistenciasApi.registrarAsistenciaDiaria(req);
      setAsistenciaMensual(prev => {
        if (!prev) return null;
        return {
          ...prev,
          asistenciasDiarias: prev.asistenciasDiarias.map(ad =>
            ad.id === registro.id ? { ...ad, estado: nuevoEstado } : ad
          ),
        };
      });
      toast.success("Asistencia actualizada");
    } catch (err) {
      console.error("Error al actualizar asistencia", err);
      toast.error("Error al actualizar la asistencia.");
    }
  };

  // Opciones para mes y año
  const meses = [
    { value: 1, label: "Enero" },
    { value: 2, label: "Febrero" },
    { value: 3, label: "Marzo" },
    { value: 4, label: "Abril" },
    { value: 5, label: "Mayo" },
    { value: 6, label: "Junio" },
    { value: 7, label: "Julio" },
    { value: 8, label: "Agosto" },
    { value: 9, label: "Septiembre" },
    { value: 10, label: "Octubre" },
    { value: 11, label: "Noviembre" },
    { value: 12, label: "Diciembre" },
  ];
  const años = Array.from({ length: 8 }, (_, i) => 2023 + i);

  // Calcular dias unicos de registros diarios para la tabla
  const diasRegistrados = useMemo(() => {
    if (!asistenciaMensual) return [];
    return Array.from(new Set(asistenciaMensual.asistenciasDiarias.map(a => a.fecha))).sort();
  }, [asistenciaMensual]);

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>
            {asistenciaMensual && asistenciaMensual.disciplina
              ? `Detalle de Asistencia Mensual - ${asistenciaMensual.disciplina}`
              : "Consultar Asistencia Mensual"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {/* Se muestran los filtros siempre (para poder modificar la consulta) */}
          <div className="mb-4 space-y-2">
            <div>
              <label htmlFor="searchDiscipline" className="block text-sm font-medium text-gray-700">
                Buscar disciplina:
              </label>
              <Input
                id="searchDiscipline"
                placeholder="Escribe el nombre..."
                value={disciplineFilter}
                onChange={(e) => setDisciplineFilter(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="disciplineSelect" className="block text-sm font-medium text-gray-700">
                Selecciona la disciplina:
              </label>
              <select
                id="disciplineSelect"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                value={selectedDisciplineId ?? ""}
                onChange={(e) => setSelectedDisciplineId(Number(e.target.value))}
              >
                <option value="">-- Seleccione --</option>
                {filteredDisciplinas.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex space-x-2">
              <div className="flex-1">
                <label htmlFor="monthSelect" className="block text-sm font-medium text-gray-700">
                  Mes:
                </label>
                <select
                  id="monthSelect"
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                  value={selectedMonth ?? ""}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                >
                  <option value="">-- Seleccione --</option>
                  {meses.map(m => (
                    <option key={m.value} value={m.value}>
                      {m.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex-1">
                <label htmlFor="yearSelect" className="block text-sm font-medium text-gray-700">
                  Año:
                </label>
                <select
                  id="yearSelect"
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                  value={selectedYear ?? ""}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                >
                  <option value="">-- Seleccione --</option>
                  {años.map(año => (
                    <option key={año} value={año}>
                      {año}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="mb-4">
              <Button onClick={cargarAsistenciaDinamica}>Consultar asistencia</Button>
            </div>
          </div>

          {loading && <div className="text-center py-4">Cargando asistencia mensual...</div>}
          {error && <div className="text-center py-4 text-red-500">{error}</div>}

          {asistenciaMensual && diasRegistrados.length === 0 && (
            <div className="text-center text-red-500 mb-4">
              No se encontraron registros diarios para este periodo.
            </div>
          )}

          {asistenciaMensual && diasRegistrados.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Alumno</TableHead>
                  {diasRegistrados.map(fecha => (
                    <TableHead key={fecha} className="text-center">
                      {new Date(fecha + "T00:00:00").toLocaleDateString("es-ES", { day: "numeric", weekday: "short" })}
                    </TableHead>
                  ))}
                  <TableHead>Observaciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {asistenciaMensual.alumnos.map(alumno => (
                  <TableRow key={alumno.id}>
                    <TableCell>{`${alumno.apellido}, ${alumno.nombre}`}</TableCell>
                    {diasRegistrados.map(fecha => {
                      const registro = asistenciaMensual.asistenciasDiarias.find(
                        ad => ad.alumnoId === alumno.id && ad.fecha === fecha
                      );
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
          )}

          <div className="mt-6 flex justify-end space-x-4">
            <Button onClick={() => navigate("/asistencias-mensuales")}>Volver</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AsistenciaMensualDetalle;
