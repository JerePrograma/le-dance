// src/funcionalidades/asistencias-diarias/AsistenciaDiariaForm.tsx
import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { Check, X } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../componentes/ui/card";
import { Button } from "../../componentes/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../componentes/ui/table";
import { Input } from "../../componentes/ui/input";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import asistenciasApi from "../../api/asistenciasApi";
import { AsistenciaDiariaRegistroRequest, AsistenciaDiariaResponse, EstadoAsistencia } from "../../types/types";
import api from "../../api/axiosConfig";

interface Disciplina {
    id: number;
    nombre: string;
    diasSemana: string[]; // Ahora son cadenas, e.g., ["JUEVES", "VIERNES"]
}

const AsistenciaDiariaForm: React.FC = () => {
    const navigate = useNavigate();

    // Estado para la lista de disciplinas y disciplina seleccionada
    const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
    const [selectedDisciplineId, setSelectedDisciplineId] = useState<number | null>(null);

    // Estado para la fecha seleccionada (por defecto, el día actual)
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    // Estado para almacenar el listado de asistencias del día (una por alumno)
    const [asistencias, setAsistencias] = useState<AsistenciaDiariaResponse[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    // Estado para las observaciones, mapeadas por alumnoId
    const [observaciones, setObservaciones] = useState<Record<number, string>>({});
    // Días de clase de la disciplina seleccionada
    const [diasClase, setDiasClase] = useState<string[]>([]);

    // Función debounce (sin librerías adicionales)
    const debounce = (func: (...args: any[]) => void, delay: number) => {
        let timer: NodeJS.Timeout;
        return (...args: any[]) => {
            if (timer) clearTimeout(timer);
            timer = setTimeout(() => func(...args), delay);
        };
    };

    // Cargar la lista de disciplinas usando asistenciasApi
    const fetchDisciplinas = useCallback(async () => {
        try {
            const data = await asistenciasApi.listarDisciplinasSimplificadas();
            setDisciplinas(data);
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

    // Cargar días de clase de la disciplina seleccionada
    const fetchDiasClase = useCallback(async () => {
        if (!selectedDisciplineId) return;
        try {
            const response = await api.get(`/disciplinas/${selectedDisciplineId}`);
            console.log("Detalle de disciplina:", response.data);
            // Ajustá el nombre de la propiedad según la respuesta:
            setDiasClase(response.data?.diasSemana || []); // o response.data?.diasClase si corresponde
        } catch (error) {
            console.error("Error al cargar días de clase:", error);
            toast.error("Error al cargar la información de la disciplina.");
        }
    }, [selectedDisciplineId]);

    useEffect(() => {
        fetchDiasClase();
    }, [fetchDiasClase]);

    // Cargar la asistencia diaria para la disciplina y fecha seleccionadas
    const cargarAsistencias = useCallback(async (fecha: Date) => {
        if (!selectedDisciplineId) return;
        setLoading(true);
        setError(null);
        try {
            const isoDate = fecha.toISOString().split("T")[0];
            // Se usa el endpoint por query parameters definido en asistenciasApi
            const pageResponse = await asistenciasApi.obtenerAsistenciasPorDisciplinaYFecha(
                selectedDisciplineId,
                isoDate,
                0,
                100
            );
            setAsistencias(pageResponse.content);
            // Inicializar observaciones a partir de cada registro (si tiene observación)
            const obs: Record<number, string> = {};
            pageResponse.content.forEach((record: AsistenciaDiariaResponse) => {
                obs[record.alumnoId] = record.observacion || "";
            });
            setObservaciones(obs);
        } catch (err) {
            console.error("Error al cargar asistencias", err);
            setError("Error al cargar la asistencia del día.");
            toast.error("No se pudo cargar la asistencia del día.");
        } finally {
            setLoading(false);
        }
    }, [selectedDisciplineId]);

    // Validar si la fecha seleccionada es un día de clase
    const isValidClassDay = useMemo(() => {
        if (!selectedDate) return false;
        if (diasClase.length === 0) return false;
        // Array de días con el mismo formato que el backend
        const dias = ["DOMINGO", "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"];
        const dayStr = dias[selectedDate.getDay()];
        return diasClase.includes(dayStr);
    }, [selectedDate, diasClase]);

    useEffect(() => {
        console.log("Fecha seleccionada:", selectedDate);
    }, [selectedDate]);

    useEffect(() => {
        if (selectedDate && isValidClassDay) {
            cargarAsistencias(selectedDate);
        } else {
            setAsistencias([]);
        }
    }, [selectedDate, isValidClassDay, cargarAsistencias]);

    // Manejar cambio en la selección de disciplina
    const handleDisciplinaChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const id = parseInt(e.target.value, 10);
        setSelectedDisciplineId(id);
        if (selectedDate) {
            cargarAsistencias(selectedDate);
        }
    };

    // Alternar el estado de asistencia para un alumno (usando asistenciasApi)
    const toggleAsistencia = async (alumnoId: number) => {
        if (!selectedDate) return;
        // Buscar el registro correspondiente
        const registro = asistencias.find(a => a.alumnoId === alumnoId);
        if (!registro) {
            toast.error("No se encontró registro de asistencia para este alumno.");
            return;
        }
        try {
            const nuevoEstado =
                registro.estado === EstadoAsistencia.PRESENTE
                    ? EstadoAsistencia.AUSENTE
                    : EstadoAsistencia.PRESENTE;
            const request: AsistenciaDiariaRegistroRequest = {
                id: registro.id,
                alumnoId: registro.alumnoId,
                fecha: selectedDate.toISOString().split("T")[0],
                estado: nuevoEstado,
                asistenciaMensualId: 0
            };
            const updated = await asistenciasApi.registrarAsistenciaDiaria(request);
            // Actualizar el registro en el estado local
            setAsistencias(prev =>
                prev.map(a => (a.id === updated.id ? updated : a))
            );
            toast.success("Asistencia actualizada");
        } catch (err) {
            console.error("Error al actualizar asistencia", err);
            toast.error("Error al actualizar la asistencia.");
        }
    };

    // Actualizar observación con debounce
    const debouncedActualizarObservacion = useCallback(
        debounce(async (alumnoId: number, obs: string) => {
            if (!selectedDate) return;
            try {
                // Se asume que existe un endpoint en asistenciasApi para actualizar observaciones
                await asistenciasApi.actualizarObservacion(0, {
                    observaciones: [{ alumnoId, observacion: obs }],
                });
                toast.success("Observación actualizada");
            } catch (err) {
                console.error("Error al actualizar observación", err);
                toast.error("Error al actualizar la observación.");
            }
        }, 500),
        [selectedDate]
    );

    const handleObservacionChange = (alumnoId: number, obs: string) => {
        setObservaciones(prev => ({ ...prev, [alumnoId]: obs }));
        debouncedActualizarObservacion(alumnoId, obs);
    };

    return (
        <div className="container mx-auto py-6">
            <Card>
                <CardHeader>
                    <CardTitle>Asistencia Diaria</CardTitle>
                </CardHeader>
                <CardContent>
                    {/* Selector de disciplina */}
                    <div className="mb-4">
                        <label
                            htmlFor="disciplinaSelect"
                            className="block text-sm font-medium text-gray-700"
                        >
                            Selecciona la disciplina:
                        </label>
                        <select
                            id="disciplinaSelect"
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                            value={selectedDisciplineId || ""}
                            onChange={handleDisciplinaChange}
                        >
                            {disciplinas.map(disciplina => (
                                <option key={disciplina.id} value={disciplina.id}>
                                    {disciplina.nombre}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* DatePicker para la fecha */}
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
                            onChange={(date: Date | null) => date && setSelectedDate(date)}
                            dateFormat="dd/MM/yyyy"
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                        />
                    </div>

                    {/* Mensaje si la fecha no es un día de clase */}
                    {!isValidClassDay && selectedDate && (
                        <div className="text-center text-red-500 mb-4">
                            No hay clases este día
                        </div>
                    )}

                    {loading && <div className="text-center py-4">Cargando asistencia...</div>}
                    {error && <div className="text-center py-4 text-red-500">{error}</div>}

                    {asistencias.length > 0 && (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Alumno</TableHead>
                                    <TableHead>Acción</TableHead>
                                    <TableHead>Observación</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {asistencias.map(record => (
                                    <TableRow key={record.id}>
                                        <TableCell>{`${record.alumnoApellido}, ${record.alumnoNombre}`}</TableCell>
                                        <TableCell className="text-center">
                                            <Button
                                                size="sm"
                                                variant={
                                                    record.estado === EstadoAsistencia.PRESENTE
                                                        ? "default"
                                                        : "outline"
                                                }
                                                onClick={() => toggleAsistencia(record.alumnoId)}
                                            >
                                                {record.estado === EstadoAsistencia.PRESENTE ? (
                                                    <Check className="h-4 w-4" />
                                                ) : (
                                                    <X className="h-4 w-4" />
                                                )}
                                            </Button>
                                        </TableCell>
                                        <TableCell>
                                            <Input
                                                placeholder="Observaciones..."
                                                value={observaciones[record.alumnoId] || ""}
                                                onChange={(e) =>
                                                    handleObservacionChange(record.alumnoId, e.target.value)
                                                }
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

export default AsistenciaDiariaForm;
