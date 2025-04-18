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
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import asistenciasApi from "../../api/asistenciasApi";
import api from "../../api/axiosConfig";
import {
  AsistenciaDiariaRegistroRequest,
  AsistenciaDiariaResponse,
  AsistenciaMensualDetalleResponse,
  EstadoAsistencia,
} from "../../types/types";

interface Disciplina {
  id: number;
  nombre: string;
  diasSemana: string[];
}

const AsistenciaDiariaFormAdaptado: React.FC = () => {
  const navigate = useNavigate();

  // Estados para filtros y datos
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const [disciplineFilter, setDisciplineFilter] = useState<string>("");
  const [selectedDisciplineId, setSelectedDisciplineId] = useState<
    number | null
  >(null);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [monthlyDetail, setMonthlyDetail] =
    useState<AsistenciaMensualDetalleResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [showSuggestions, setShowSuggestions] = useState<boolean>(false);
  const [activeSuggestionIndex, setActiveSuggestionIndex] =
    useState<number>(-1);
  const searchWrapperRef = useRef<HTMLDivElement>(null);
  const [isValidClassDay, setIsValidClassDay] = useState<boolean>(false);
  const [, setDiasClase] = useState<string[]>([]);

  // Cargar lista de disciplinas
  const fetchDisciplinas = useCallback(async () => {
    try {
      const data = await asistenciasApi.listarDisciplinasSimplificadas();
      const disciplinasConDias = data.map((d: any) => ({
        ...d,
        diasSemana: d.diasSemana || [],
      }));
      setDisciplinas(disciplinasConDias);
    } catch (err) {}
  }, []);

  useEffect(() => {
    fetchDisciplinas();
  }, [fetchDisciplinas]);

  const filteredDisciplinas = useMemo(() => {
    if (!disciplineFilter.trim()) return disciplinas;
    return disciplinas.filter((d) =>
      d.nombre.toLowerCase().includes(disciplineFilter.toLowerCase())
    );
  }, [disciplineFilter, disciplinas]);

  // Obtiene los días de clase de la disciplina seleccionada
  const fetchDiasClase = useCallback(async (): Promise<string[]> => {
    if (!selectedDisciplineId) return [];
    try {
      const response = await api.get(`/disciplinas/${selectedDisciplineId}`);
      const horarios = response.data?.horarios || [];
      const dias = horarios.map((h: any) => h.diaSemana);
      setDiasClase(dias);
      return dias;
    } catch (error) {
      return [];
    }
  }, [selectedDisciplineId]);

  const handleSeleccionarDisciplina = (disciplina: Disciplina) => {
    setSelectedDisciplineId(disciplina.id);
    setDisciplineFilter(disciplina.nombre);
    setActiveSuggestionIndex(-1);
    setShowSuggestions(false);
    fetchDiasClase();
  };

  const handleDisciplineKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>
  ) => {
    if (filteredDisciplinas.length > 0) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev < filteredDisciplinas.length - 1 ? prev + 1 : 0
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveSuggestionIndex((prev) =>
          prev > 0 ? prev - 1 : filteredDisciplinas.length - 1
        );
      } else if (e.key === "Enter" || e.key === "Tab") {
        if (
          activeSuggestionIndex >= 0 &&
          activeSuggestionIndex < filteredDisciplinas.length
        ) {
          e.preventDefault();
          handleSeleccionarDisciplina(
            filteredDisciplinas[activeSuggestionIndex]
          );
        }
      }
    }
  };

  const limpiarDisciplina = () => {
    setDisciplineFilter("");
    setSelectedDisciplineId(null);
    setShowSuggestions(false);
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        searchWrapperRef.current &&
        !searchWrapperRef.current.contains(e.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleBuscarAsistencia = async () => {
    if (!selectedDate || !selectedDisciplineId) {
      toast.warn("Complete los filtros");
      return;
    }
    const dias = await fetchDiasClase();
    const diasAbreviados = dias.map((d) => d.substring(0, 3).toUpperCase());
    const diasReferencia = ["DOM", "LUN", "MAR", "MIE", "JUE", "VIE", "SAB"];
    const dayStr = diasReferencia[selectedDate.getDay()];
    if (!diasAbreviados.includes(dayStr)) {
      setIsValidClassDay(false);
      setMonthlyDetail(null);
      return;
    }
    setIsValidClassDay(true);
    cargarAsistencias(selectedDate);
  };

  const cargarAsistencias = useCallback(
    async (fecha: Date) => {
      if (!selectedDisciplineId) return;
      setLoading(true);
      setError(null);
      try {
        const mes = fecha.getMonth() + 1;
        const anio = fecha.getFullYear();
        const detail =
          await asistenciasApi.obtenerAsistenciaMensualDetallePorParametros(
            selectedDisciplineId,
            mes,
            anio
          );
        setMonthlyDetail(detail);
      } catch (err) {
      } finally {
        setLoading(false);
      }
    },
    [selectedDisciplineId]
  );

  // Deduplicamos los registros de alumnos basándonos en el ID del alumno
  const uniqueAlumnos = useMemo(() => {
    if (!monthlyDetail) return [];
    const alumnosMap = new Map<number, (typeof monthlyDetail.alumnos)[0]>();
    monthlyDetail.alumnos.forEach((alumno) => {
      const alumnoId = alumno.alumno?.id;
      if (alumnoId) {
        if (!alumnosMap.has(alumnoId)) {
          alumnosMap.set(alumnoId, { ...alumno });
        } else {
          const existing = alumnosMap.get(alumnoId)!;
          existing.asistenciasDiarias = [
            ...existing.asistenciasDiarias,
            ...alumno.asistenciasDiarias,
          ];
          if (!existing.observacion && alumno.observacion) {
            existing.observacion = alumno.observacion;
          }
        }
      } else {
        alumnosMap.set(alumno.id, alumno);
      }
    });
    return Array.from(alumnosMap.values());
  }, [monthlyDetail]);

  // Generamos la lista de registros diarios a partir de los alumnos únicos
  const dailyRecords = useMemo(() => {
    if (!monthlyDetail) return [];
    const selectedIso = selectedDate.toISOString().split("T")[0];
    return uniqueAlumnos.map((alumno) => {
      // Buscamos el registro de asistencia para la fecha seleccionada
      const asistenciaDiaria = alumno.asistenciasDiarias.find(
        (ad) => ad.fecha === selectedIso
      );
      // Usamos directamente la información mapeada en "alumno"
      const alumnoNombre = alumno.alumno?.nombre || "";
      const alumnoApellido = alumno.alumno?.apellido || "";
      return {
        alumnoId: alumno.id,
        alumnoNombre,
        alumnoApellido,
        asistenciaDiaria,
      };
    });
  }, [monthlyDetail, selectedDate, uniqueAlumnos]);

  const toggleAsistencia = async (
    alumnoId: number,
    currentRecord: AsistenciaDiariaResponse | undefined
  ) => {
    if (!selectedDate || !monthlyDetail) return;
    const fechaFormateada = selectedDate.toISOString().split("T")[0];
    if (!currentRecord) {
      const alumnoRegistro = uniqueAlumnos.find((a) => a.id === alumnoId);
      if (!alumnoRegistro) {
        return;
      }
      const newRecord: AsistenciaDiariaRegistroRequest = {
        fecha: fechaFormateada,
        estado: EstadoAsistencia.PRESENTE,
        asistenciaAlumnoMensualId: alumnoRegistro.id,
      };
      try {
        const created = await asistenciasApi.registrarAsistenciaDiaria(
          newRecord
        );
        const updatedAlumnos = monthlyDetail.alumnos.map((alumno) => {
          if (alumno.id === alumnoId) {
            return {
              ...alumno,
              asistenciasDiarias: [...alumno.asistenciasDiarias, created],
            };
          }
          return alumno;
        });
        setMonthlyDetail({ ...monthlyDetail, alumnos: updatedAlumnos });
      } catch (err) {}
      return;
    }
    try {
      const nuevoEstado =
        currentRecord.estado === EstadoAsistencia.PRESENTE
          ? EstadoAsistencia.AUSENTE
          : EstadoAsistencia.PRESENTE;
      const request: AsistenciaDiariaRegistroRequest = {
        id: currentRecord.id,
        fecha: fechaFormateada,
        estado: nuevoEstado,
        asistenciaAlumnoMensualId: currentRecord.asistenciaAlumnoMensualId,
      };
      const updated = await asistenciasApi.registrarAsistenciaDiaria(request);
      const updatedAlumnos = monthlyDetail.alumnos.map((alumno) => {
        if (alumno.id === alumnoId) {
          return {
            ...alumno,
            asistenciasDiarias: alumno.asistenciasDiarias.map((ad) =>
              ad.fecha === fechaFormateada ? updated : ad
            ),
          };
        }
        return alumno;
      });
      setMonthlyDetail({ ...monthlyDetail, alumnos: updatedAlumnos });
    } catch (err) {}
  };

  const debounce = (func: (...args: any[]) => void, delay: number) => {
    let timer: NodeJS.Timeout;
    return (...args: any[]) => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => func(...args), delay);
    };
  };

  const debouncedActualizarObservacion = useCallback(
    debounce(async (alumnoId: number, obs: string) => {
      if (!monthlyDetail) return;
      const alumno = monthlyDetail.alumnos.find((a) => a.id === alumnoId);
      if (!alumno) return;
      const payload = {
        asistenciasAlumnoMensual: [
          { id: alumno.id, observacion: obs, asistenciasDiarias: [] },
        ],
      };
      try {
        await asistenciasApi.actualizarAsistenciaMensual(
          monthlyDetail.id,
          payload
        );
      } catch (err) {}
    }, 500),
    [monthlyDetail]
  );

  const handleObservacionChange = (alumnoId: number, newObs: string) => {
    if (monthlyDetail) {
      const updatedAlumnos = monthlyDetail.alumnos.map((alumno) => {
        if (alumno.id === alumnoId) {
          return { ...alumno, observacion: newObs };
        }
        return alumno;
      });
      setMonthlyDetail({ ...monthlyDetail, alumnos: updatedAlumnos });
    }
    debouncedActualizarObservacion(alumnoId, newObs);
  };

  const formatHeaderDate = (date: Date): string => {
    const weekday = date
      .toLocaleDateString("es-ES", { weekday: "short" })
      .replace(".", "");
    const day = date.toLocaleDateString("es-ES", { day: "numeric" });
    return `${weekday} ${day}`;
  };

  return (
    <div className="container mx-auto py-6">
      <Card>
        <CardHeader>
          <CardTitle>Asistencia Diaria</CardTitle>
        </CardHeader>
        <CardContent>
          {/* Filtros: búsqueda de disciplina y selección de fecha */}
          <div className="mb-4" ref={searchWrapperRef}>
            <label
              htmlFor="searchDiscipline"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Selecciona la disciplina:
            </label>
            <div className="relative">
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
                      onMouseEnter={() => setActiveSuggestionIndex(index)}
                      className={`p-2 cursor-pointer ${
                        index === activeSuggestionIndex ? "bg-gray-200" : ""
                      }`}
                    >
                      {disciplina.nombre}
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <Button
              onClick={limpiarDisciplina}
              variant="outline"
              size="sm"
              className="mt-2"
            >
              Limpiar
            </Button>
          </div>

          <div className="mb-4">
            <label
              htmlFor="datePicker"
              className="block text-sm font-medium text-gray-700"
            >
              Selecciona la fecha:
            </label>
            <DatePicker
              id="datePicker"
              selected={selectedDate}
              onChange={(date: Date | null) =>
                date && setSelectedDate(new Date(date))
              }
              dateFormat="dd/MM/yyyy"
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
            />
          </div>

          <div className="mb-4">
            <Button onClick={handleBuscarAsistencia}>Buscar asistencia</Button>
          </div>

          {!loading && selectedDate && !isValidClassDay && (
            <div className="text-center text-red-500 mb-4">
              No hay clases este día
            </div>
          )}

          {loading && (
            <div className="text-center py-4">Cargando asistencia...</div>
          )}
          {error && (
            <div className="text-center py-4 text-red-500">{error}</div>
          )}

          {monthlyDetail && (
            <Table key={selectedDate.toISOString()}>
              <TableHeader>
                <TableRow>
                  <TableHead>Alumno</TableHead>
                  <TableHead className="text-center">
                    {selectedDate ? formatHeaderDate(selectedDate) : "Acción"}
                  </TableHead>
                  <TableHead>Observación</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {dailyRecords.map((record) => (
                  <TableRow key={record.alumnoId}>
                    <TableCell>{`${record.alumnoApellido}, ${record.alumnoNombre}`}</TableCell>
                    <TableCell className="text-center">
                      <Button
                        size="sm"
                        variant={
                          record.asistenciaDiaria &&
                          record.asistenciaDiaria.estado ===
                            EstadoAsistencia.PRESENTE
                            ? "default"
                            : "outline"
                        }
                        onClick={() =>
                          toggleAsistencia(
                            record.alumnoId,
                            record.asistenciaDiaria
                          )
                        }
                      >
                        {record.asistenciaDiaria &&
                        record.asistenciaDiaria.estado ===
                          EstadoAsistencia.PRESENTE ? (
                          <Check className="h-4 w-4" />
                        ) : (
                          <X className="h-4 w-4" />
                        )}
                      </Button>
                    </TableCell>
                    <TableCell>
                      <Input
                        placeholder="Escribe observación..."
                        value={
                          monthlyDetail.alumnos.find(
                            (a) => a.id === record.alumnoId
                          )?.observacion || ""
                        }
                        onChange={(e) =>
                          handleObservacionChange(
                            record.alumnoId,
                            e.target.value
                          )
                        }
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          <div className="mt-6 flex justify-end space-x-4">
            <Button onClick={() => navigate("/asistencias-mensuales")}>
              Volver
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AsistenciaDiariaFormAdaptado;
