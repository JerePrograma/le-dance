import React, {
  useEffect,
  useState,
  useCallback,
  useMemo,
  useRef,
} from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { Check, X } from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../../componentes/ui/card";
import { Button } from "../../componentes/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../componentes/ui/table";
import { Input } from "../../componentes/ui/input";
import asistenciasApi from "../../api/asistenciasApi";
import {
  AsistenciaMensualDetalleResponse,
  EstadoAsistencia,
} from "../../types/types";
import type { DisciplinaListadoResponse } from "../../types/types";
import useDebounce from "../../hooks/useDebounce";

// Helper: devuelve el nombre del alumno o un valor por defecto si no se encuentra
const getAlumnoNombre = (alumnoRecord: any): string => {
  // Si el objeto tiene la propiedad "alumno" y dentro de ella "nombre", se usa esa info
  if (alumnoRecord.alumno && alumnoRecord.alumno.nombre && alumnoRecord.alumno.nombre.trim() !== "") {
    return alumnoRecord.alumno.nombre;
  }
  // Si hay registros de asistencias y en el primero se encuentra el objeto alumno
  if (alumnoRecord.asistenciasDiarias &&
    alumnoRecord.asistenciasDiarias.length > 0 &&
    alumnoRecord.asistenciasDiarias[0].alumno &&
    alumnoRecord.asistenciasDiarias[0].alumno.nombre &&
    alumnoRecord.asistenciasDiarias[0].alumno.nombre.trim() !== "") {
    return alumnoRecord.asistenciasDiarias[0].alumno.nombre;
  }
  return "Sin nombre";
};

// Helper: devuelve el apellido del alumno o un valor por defecto
const getAlumnoApellido = (alumnoRecord: any): string => {
  if (alumnoRecord.alumno && alumnoRecord.alumno.apellido && alumnoRecord.alumno.apellido.trim() !== "") {
    return alumnoRecord.alumno.apellido;
  }
  if (alumnoRecord.asistenciasDiarias &&
    alumnoRecord.asistenciasDiarias.length > 0 &&
    alumnoRecord.asistenciasDiarias[0].alumno &&
    alumnoRecord.asistenciasDiarias[0].alumno.apellido &&
    alumnoRecord.asistenciasDiarias[0].alumno.apellido.trim() !== "") {
    return alumnoRecord.asistenciasDiarias[0].alumno.apellido;
  }
  return "Sin apellido";
};

const AsistenciaMensualDetalle: React.FC = () => {
  const navigate = useNavigate();

  // Estados para filtros
  const [disciplinas, setDisciplinas] = useState<DisciplinaListadoResponse[]>([]);
  const [disciplineFilter, setDisciplineFilter] = useState<string>("");
  const [selectedDisciplineId, setSelectedDisciplineId] = useState<number | null>(null);
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState<number>(currentDate.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState<number>(currentDate.getFullYear());

  // Estado para el detalle de asistencia mensual
  const [asistenciaMensual, setAsistenciaMensual] = useState<AsistenciaMensualDetalleResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  // Mapa de observaciones por alumno
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});

  // Estados para el sistema de búsqueda (select con sugerencias)
  const [showSuggestions, setShowSuggestions] = useState<boolean>(false);
  const [activeDisciplineSuggestionIndex, setActiveDisciplineSuggestionIndex] = useState<number>(-1);
  const searchWrapperRef = useRef<HTMLDivElement>(null);

  // Cargar disciplinas
  const fetchDisciplinas = useCallback(async () => {
    try {
      const data = await asistenciasApi.listarDisciplinasSimplificadas();
      setDisciplinas(data);
    } catch (err) {
      console.error("Error al cargar disciplinas", err);
      toast.error("Error al cargar la lista de disciplinas.");
    }
  }, []);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  // Debounce para el filtro de disciplinas
  const debouncedDisciplineFilter = useDebounce(disciplineFilter, 300);

  const filteredDisciplinas = useMemo(() => {
    if (!debouncedDisciplineFilter.trim()) return disciplinas;
    return disciplinas.filter((d) =>
      d.nombre.toLowerCase().includes(debouncedDisciplineFilter.toLowerCase())
    );
  }, [debouncedDisciplineFilter, disciplinas]);

  const handleSeleccionarDisciplina = (disciplina: DisciplinaListadoResponse) => {
    setSelectedDisciplineId(disciplina.id);
    setDisciplineFilter(disciplina.nombre);
    setActiveDisciplineSuggestionIndex(-1);
    setShowSuggestions(false);
  };

  const handleDisciplineKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (filteredDisciplinas.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveDisciplineSuggestionIndex((prev) =>
          prev < filteredDisciplinas.length - 1 ? prev + 1 : 0
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveDisciplineSuggestionIndex((prev) =>
          prev > 0 ? prev - 1 : filteredDisciplinas.length - 1
        );
      } else if (e.key === "Enter" || e.key === "Tab") {
        if (
          activeDisciplineSuggestionIndex >= 0 &&
          activeDisciplineSuggestionIndex < filteredDisciplinas.length
        ) {
          e.preventDefault();
          handleSeleccionarDisciplina(filteredDisciplinas[activeDisciplineSuggestionIndex]);
        }
      }
    }
  };

  // Función para limpiar la disciplina buscada
  const limpiarDisciplina = () => {
    setDisciplineFilter("");
    setSelectedDisciplineId(null);
    setShowSuggestions(false);
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchWrapperRef.current && !searchWrapperRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  // Consultar el detalle mensual según disciplina, mes y año
  const cargarAsistenciaDinamica = useCallback(async () => {
    if (!selectedDisciplineId || !selectedMonth || !selectedYear) {
      toast.warn("Complete todos los filtros antes de consultar.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await asistenciasApi.obtenerAsistenciaMensualDetallePorParametros(
        selectedDisciplineId,
        selectedMonth,
        selectedYear
      );
      if (data) {
        setAsistenciaMensual(data);
        // Construir mapa de observaciones a partir de cada registro de alumno
        const obsMap: Record<number, string> = {};
        data.alumnos.forEach((alumno) => {
          obsMap[alumno.id] = alumno.observacion || "";
        });
        setObservaciones(obsMap);
      } else {
        setAsistenciaMensual(null);
        toast.info("No se encontró asistencia mensual para estos parámetros.");
      }
    } catch (err) {
      console.error("Error al cargar la asistencia mensual", err);
      toast.error("No se pudo cargar la asistencia mensual.");
    } finally {
      setLoading(false);
    }
  }, [selectedDisciplineId, selectedMonth, selectedYear]);

  // Calculamos los días registrados a partir de los registros diarios
  const diasRegistrados = useMemo(() => {
    if (!asistenciaMensual) return [];
    const fechasSet = new Set<string>();
    asistenciaMensual.alumnos.forEach((alumno) => {
      alumno.asistenciasDiarias.forEach((ad) => fechasSet.add(ad.fecha));
    });
    return Array.from(fechasSet).sort();
  }, [asistenciaMensual]);

  // Debounce para actualizar observaciones
  const debouncedActualizarObservacion = useCallback(
    (alumnoId: number, obs: string) => {
      const asistenciasAlumnoMensualArray = Object.entries({ ...observaciones, [alumnoId]: obs }).map(
        ([id, observacion]) => ({
          id: Number(id),
          observacion,
          asistenciasDiarias: [] // Se envía vacío si no se actualizan
        })
      );
      asistenciasApi
        .actualizarAsistenciaMensual(asistenciaMensual!.id, { asistenciasAlumnoMensual: asistenciasAlumnoMensualArray })
        .then(() => toast.success("Observación actualizada"))
        .catch((err) => {
          console.error("Error al actualizar observación", err);
          toast.error("Error al actualizar la observación.");
        });
    },
    [asistenciaMensual, observaciones]
  );

  const handleObservacionChange = (alumnoId: number, obs: string) => {
    setObservaciones((prev) => ({ ...prev, [alumnoId]: obs }));
    debouncedActualizarObservacion(alumnoId, obs);
  };

  // Alternar la asistencia diaria de un alumno en una fecha determinada
  const toggleAsistencia = async (alumnoId: number, fecha: string) => {
    if (!asistenciaMensual) return;
    const alumnoRegistro = asistenciaMensual.alumnos.find((al) => al.id === alumnoId);
    if (!alumnoRegistro) {
      toast.error("Registro de alumno no encontrado.");
      return;
    }
    const registro = alumnoRegistro.asistenciasDiarias.find((ad) => ad.fecha === fecha);
    if (!registro) {
      toast.error("No se encontró registro de asistencia para este alumno en esta fecha.");
      return;
    }
    try {
      const nuevoEstado =
        registro.estado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE;

      const request = {
        id: registro.id,
        fecha, // Formato ISO (YYYY-MM-DD)
        estado: nuevoEstado,
        asistenciaAlumnoMensualId: registro.asistenciaAlumnoMensualId,
      };

      const updated = await asistenciasApi.registrarAsistenciaDiaria(request);

      // Actualizamos el estado local según la respuesta del API
      setAsistenciaMensual((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          alumnos: prev.alumnos.map((al) =>
            al.id === alumnoId
              ? {
                ...al,
                asistenciasDiarias: al.asistenciasDiarias.map((ad) =>
                  ad.id === registro.id ? updated : ad
                ),
              }
              : al
          ),
        };
      });
      toast.success("Asistencia actualizada");
    } catch (err) {
      console.error("Error al actualizar asistencia", err);
      toast.error("Error al actualizar la asistencia.");
    }
  };

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>
            {asistenciaMensual && asistenciaMensual.disciplina
              ? `Detalle de Asistencia Mensual - ${asistenciaMensual.disciplina.nombre}`
              : "Consultar Asistencia Mensual"}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {/* Filtros: búsqueda de disciplina y selección de mes/año */}
          <div className="mb-4 space-y-2">
            <div ref={searchWrapperRef} className="flex items-center space-x-2">
              <div className="flex-1">
                <label htmlFor="searchDiscipline" className="block text-sm font-medium text-gray-700 mb-1">
                  Buscar disciplina:
                </label>
                <Input
                  id="searchDiscipline"
                  placeholder="Escribe el nombre..."
                  value={disciplineFilter}
                  onChange={(e) => {
                    setDisciplineFilter(e.target.value);
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  onKeyDown={handleDisciplineKeyDown}
                  className="form-input w-full"
                />
                {showSuggestions && filteredDisciplinas.length > 0 && (
                  <ul className="sugerencias-lista absolute z-10 w-full bg-white text-black border">
                    {filteredDisciplinas.map((disciplina, index) => (
                      <li
                        key={disciplina.id}
                        onClick={() => handleSeleccionarDisciplina(disciplina)}
                        onMouseEnter={() => setActiveDisciplineSuggestionIndex(index)}
                        className={`p-2 cursor-pointer ${index === activeDisciplineSuggestionIndex ? "bg-gray-200" : ""}`}
                      >
                        {disciplina.nombre}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <Button onClick={limpiarDisciplina} variant="outline" size="sm">
                Limpiar
              </Button>
            </div>
            <div className="flex space-x-2">
              <div className="flex-1">
                <label htmlFor="monthSelect" className="block text-sm font-medium text-gray-700">
                  Mes:
                </label>
                <select
                  id="monthSelect"
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                  value={selectedMonth || ""}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                >
                  <option value="">-- Seleccione --</option>
                  {[
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
                  ].map((m) => (
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
                  value={selectedYear || ""}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                >
                  <option value="">-- Seleccione --</option>
                  {Array.from({ length: 8 }, (_, i) => currentDate.getFullYear() + i).map((año) => (
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
                  {diasRegistrados.map((fecha) => (
                    <TableHead key={fecha} className="text-center">
                      {new Date(fecha + "T00:00:00").toLocaleDateString("es-ES", {
                        day: "numeric",
                        weekday: "short",
                      })}
                    </TableHead>
                  ))}
                  <TableHead>Observación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {asistenciaMensual.alumnos.map((alumno) => (
                  <TableRow key={alumno.id}>
                    <TableCell>
                      {`${getAlumnoApellido(alumno)}, ${getAlumnoNombre(alumno)}`}
                    </TableCell>
                    {diasRegistrados.map((fecha) => {
                      const registro = alumno.asistenciasDiarias.find((ad) => ad.fecha === fecha);
                      return (
                        <TableCell key={fecha} className="text-center">
                          <Button
                            size="sm"
                            variant={registro?.estado === EstadoAsistencia.PRESENTE ? "default" : "outline"}
                            onClick={() => registro && toggleAsistencia(alumno.id, fecha)}
                          >
                            {registro?.estado === EstadoAsistencia.PRESENTE ? (
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
