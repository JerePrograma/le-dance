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
    diasSemana: string[];
}

const AsistenciaDiariaForm: React.FC = () => {
    const navigate = useNavigate();

    // Estados para filtros y datos
    const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
    const [selectedDisciplineId, setSelectedDisciplineId] = useState<number | null>(null);
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [asistencias, setAsistencias] = useState<AsistenciaDiariaResponse[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [, setErrorMessage] = useState<string | null>(null); // Estado para mensaje de error visual
    const [observaciones, setObservaciones] = useState<Record<number, string>>({});
    const [diasClase, setDiasClase] = useState<string[]>([]);

    const debounce = (func: (...args: any[]) => void, delay: number) => {
        let timer: NodeJS.Timeout;
        return (...args: any[]) => {
            if (timer) clearTimeout(timer);
            timer = setTimeout(() => func(...args), delay);
        };
    };

    const fetchDisciplinas = useCallback(async () => {
        try {
            const data = await asistenciasApi.listarDisciplinasSimplificadas();
            // Mapea cada elemento agregando diasSemana (por defecto, vacío o con otro valor que necesites)
            const disciplinasConDias = data.map((d: any) => ({
                ...d,
                diasSemana: [], // o un array con valores por defecto si aplica
            }));
            setDisciplinas(disciplinasConDias);
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

    const fetchDiasClase = useCallback(async () => {
        if (!selectedDisciplineId) return;
        try {
            const response = await api.get(`/disciplinas/${selectedDisciplineId}`);
            console.log("Detalle de disciplina:", response.data);
            setDiasClase(response.data?.diasSemana || []);
        } catch (error) {
            console.error("Error al cargar días de clase:", error);
            toast.error("Error al cargar la información de la disciplina.");
        }
    }, [selectedDisciplineId]);

    useEffect(() => {
        fetchDiasClase();
    }, [fetchDiasClase]);

    const cargarAsistencias = useCallback(async (fecha: Date) => {
        if (!selectedDisciplineId) return;
        setLoading(true);
        setError(null);
        try {
            const isoDate = fecha.toISOString().split("T")[0];
            const pageResponse = await asistenciasApi.obtenerAsistenciasPorDisciplinaYFecha(
                selectedDisciplineId,
                isoDate,
                0,
                100
            );
            setAsistencias(pageResponse.content);
            const obs: Record<number, string> = {};
            pageResponse.content.forEach((record: AsistenciaDiariaResponse) => {
                obs[record.alumnoId] = record.observacion || "";
            });
            setObservaciones(obs);
            // Si no hay registros y se espera clase, establecemos el mensaje de error
            if (pageResponse.content.length === 0) {
                setErrorMessage("No hay clases este día");
            } else {
                setErrorMessage(null);
            }
        } catch (err) {
            console.error("Error al cargar asistencias", err);
            setError("Error al cargar la asistencia del día.");
            toast.error("No se pudo cargar la asistencia del día.");
        } finally {
            setLoading(false);
        }
    }, [selectedDisciplineId]);

    // Función para parsear la fecha en zona local
    const parseLocalDate = (dateStr: string): Date => {
        const [year, month, day] = dateStr.split("-").map(Number);
        return new Date(year, month - 1, day);
    };

    const isValidClassDay = useMemo(() => {
        if (!selectedDate) return false;
        if (diasClase.length === 0) return false;
        const localDate = parseLocalDate(selectedDate.toISOString().split("T")[0]);
        const dias = ["DOMINGO", "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"];
        const dayStr = dias[localDate.getDay()];
        return diasClase.includes(dayStr);
    }, [selectedDate, diasClase]);

    useEffect(() => {
        console.log("Fecha seleccionada:", selectedDate);
    }, [selectedDate]);

    useEffect(() => {
        console.log("useEffect: selectedDate cambió a:", selectedDate);
        if (selectedDate && isValidClassDay) {
            cargarAsistencias(selectedDate);
        } else {
            setAsistencias([]);
        }
    }, [selectedDate, isValidClassDay, cargarAsistencias]);

    const handleDisciplinaChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const id = parseInt(e.target.value, 10);
        setSelectedDisciplineId(id);
        if (selectedDate) {
            cargarAsistencias(selectedDate);
        }
    };

    const toggleAsistencia = async (alumnoId: number) => {
        if (!selectedDate) return;
        const registro = asistencias.find(a => a.alumnoId === alumnoId);
        if (!registro) {
            toast.error("No se encontró registro de asistencia para este alumno.");
            return;
        }
        try {
            const nuevoEstado =
                registro.estado === EstadoAsistencia.PRESENTE ? EstadoAsistencia.AUSENTE : EstadoAsistencia.PRESENTE;
            const request: AsistenciaDiariaRegistroRequest = {
                id: registro.id,
                alumnoId: registro.alumnoId,
                fecha: selectedDate.toISOString().split("T")[0],
                estado: nuevoEstado,
                asistenciaMensualId: 0
            };
            const updated = await asistenciasApi.registrarAsistenciaDiaria(request);
            setAsistencias(prev => prev.map(a => (a.id === updated.id ? updated : a)));
            toast.success("Asistencia actualizada");
        } catch (err) {
            console.error("Error al actualizar asistencia", err);
            toast.error("Error al actualizar la asistencia.");
        }
    };

    const debouncedActualizarObservacion = useCallback(
        debounce(async (alumnoId: number, obs: string) => {
            if (!selectedDate) return;
            try {
                await asistenciasApi.actualizarObservacion({
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
                        <label htmlFor="disciplinaSelect" className="block text-sm font-medium text-gray-700">
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
                        <label htmlFor="datePicker" className="block text-sm font-medium text-gray-700">
                            Selecciona la fecha:
                        </label>
                        <DatePicker
                            id="datePicker"
                            selected={selectedDate}
                            onChange={(date: Date | null) => date && setSelectedDate(new Date(date))}
                            dateFormat="dd/MM/yyyy"
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm"
                        />
                    </div>

                    {/* Botón para buscar asistencia */}
                    <div className="mb-4">
                        <Button
                            onClick={() => {
                                if (!selectedDate || !selectedDisciplineId) {
                                    toast.warn("Complete los filtros");
                                    return;
                                }
                                cargarAsistencias(selectedDate);
                            }}
                        >
                            Buscar asistencia
                        </Button>
                    </div>

                    {/* Mostrar mensaje de error si no hay clases */}
                    {!loading && selectedDate && asistencias.length === 0 && (
                        <div className="text-center text-red-500 mb-4">No hay clases este día</div>
                    )}

                    {loading && <div className="text-center py-4">Cargando asistencia...</div>}
                    {error && <div className="text-center py-4 text-red-500">{error}</div>}

                    {asistencias.length > 0 && (
                        <Table key={selectedDate.toISOString()}>
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
                                                variant={record.estado === EstadoAsistencia.PRESENTE ? "default" : "outline"}
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
                                                onChange={(e) => handleObservacionChange(record.alumnoId, e.target.value)}
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
