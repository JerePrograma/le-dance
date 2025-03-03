// ReporteDetallePago.tsx
import React, { useState, useEffect } from "react";
import { useFormik } from "formik";
import * as Yup from "yup";
import useDebounce from "../../hooks/useDebounce";
import disciplinasApi from "../../api/disciplinasApi";
import profesoresApi from "../../api/profesoresApi";
import type { ReporteMensualidadDTO } from "../../types/types";
import reporteMensualidadApi from "../../api/reporteMensualidadApi";
import Tabla from "../../componentes/comunes/Tabla";

interface FiltrosBusqueda {
    // Usaremos inputs tipo "month" que devuelven "YYYY-MM"
    fechaInicio: string;
    fechaFin: string;
    disciplinaNombre: string; // Busqueda por nombre de disciplina
    profesorNombre: string;   // Busqueda por nombre de profesor
}

// Establecemos el mes actual en formato "YYYY-MM" como valor inicial
const mesActual = new Date().toISOString().substring(0, 7);

const ReporteDetallePago: React.FC = () => {
    const [resultados, setResultados] = useState<ReporteMensualidadDTO[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    // Estados para sugerencias de disciplina y profesor
    const [sugerenciasDisciplinas, setSugerenciasDisciplinas] = useState<any[]>([]);
    const [sugerenciasProfesores, setSugerenciasProfesores] = useState<any[]>([]);

    // Hooks de debounce para disciplina y profesor
    const [disciplinaBusqueda, setDisciplinaBusqueda] = useState<string>("");
    const debouncedDisciplinaBusqueda = useDebounce(disciplinaBusqueda, 300);

    const [profesorBusqueda, setProfesorBusqueda] = useState<string>("");
    const debouncedProfesorBusqueda = useDebounce(profesorBusqueda, 300);

    // Configuracion de Formik para los filtros
    const formik = useFormik<FiltrosBusqueda>({
        initialValues: {
            fechaInicio: mesActual,
            fechaFin: mesActual,
            disciplinaNombre: "",
            profesorNombre: "",
        },
        validationSchema: Yup.object({
            fechaInicio: Yup.string().required("El mes de inicio es obligatorio"),
            fechaFin: Yup.string().required("El mes fin es obligatorio"),
        }),
        // ReporteDetallePago.tsx (fragmento relevante)
        // ...
        onSubmit: async (values) => {
            setLoading(true);
            setError(null);
            try {
                // Funcion para transformar "YYYY-MM" en fecha inicio (primer dia) y fecha fin (ultimo dia)
                const transformarMesAFechas = (mesStr: string): { inicio: string; fin: string } => {
                    const [year, month] = mesStr.split("-").map(Number);
                    const inicio = new Date(year, month - 1, 1);
                    const fin = new Date(year, month, 0); // Último dia del mes
                    const formatear = (d: Date) =>
                        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
                    return { inicio: formatear(inicio), fin: formatear(fin) };
                };

                const { inicio: inicioFecha } = transformarMesAFechas(values.fechaInicio);
                const { fin: finFecha } = transformarMesAFechas(values.fechaFin);

                const params = {
                    fechaInicio: inicioFecha,
                    fechaFin: finFecha,
                    disciplinaNombre: values.disciplinaNombre || undefined,
                    profesorNombre: values.profesorNombre || undefined,
                };

                console.log("Llamando a /api/reportes/mensualidades/buscar con parametros:", params);

                // Se espera directamente un array de ReporteMensualidadDTO
                const response: ReporteMensualidadDTO[] = await reporteMensualidadApi.listarReporte(params);
                console.log("Respuesta recibida:", response);
                setResultados(response);
            } catch (err: any) {
                console.error("Error al obtener el reporte:", err);
                setError("Error al cargar los datos del reporte");
            } finally {
                setLoading(false);
            }
        },
    });

    // useEffect para sugerencias de disciplinas
    useEffect(() => {
        const buscarSugerenciasDisciplinas = async () => {
            if (debouncedDisciplinaBusqueda) {
                try {
                    const sugerencias = await disciplinasApi.buscarPorNombre(debouncedDisciplinaBusqueda);
                    console.log("Sugerencias de disciplinas:", sugerencias);
                    setSugerenciasDisciplinas(sugerencias);
                } catch (err) {
                    console.error("Error al buscar sugerencias de disciplinas:", err);
                    setSugerenciasDisciplinas([]);
                }
            } else {
                setSugerenciasDisciplinas([]);
            }
        };
        buscarSugerenciasDisciplinas();
    }, [debouncedDisciplinaBusqueda]);

    // useEffect para sugerencias de profesores
    useEffect(() => {
        const buscarSugerenciasProfesores = async () => {
            if (debouncedProfesorBusqueda) {
                try {
                    const sugerencias = await profesoresApi.buscarPorNombre(debouncedProfesorBusqueda);
                    console.log("Sugerencias de profesores:", sugerencias);
                    setSugerenciasProfesores(sugerencias);
                } catch (err) {
                    console.error("Error al buscar sugerencias de profesores:", err);
                    setSugerenciasProfesores([]);
                }
            } else {
                setSugerenciasProfesores([]);
            }
        };
        buscarSugerenciasProfesores();
    }, [debouncedProfesorBusqueda]);

    return (
        <div className="p-4">
            <h1 className="text-2xl font-bold mb-4">Reporte de Mensualidades (Detalle de Pagos)</h1>
            <form onSubmit={formik.handleSubmit} className="mb-4 grid grid-cols-2 gap-4 relative">
                {/* Filtros de Fecha usando input type "month" */}
                <div>
                    <label className="block font-medium">Mes Inicio:</label>
                    <input
                        type="month"
                        name="fechaInicio"
                        onChange={formik.handleChange}
                        onBlur={formik.handleBlur}
                        value={formik.values.fechaInicio}
                        className="border p-2 w-full"
                    />
                    {formik.touched.fechaInicio && formik.errors.fechaInicio && (
                        <div className="text-red-500">{formik.errors.fechaInicio}</div>
                    )}
                </div>
                <div>
                    <label className="block font-medium">Mes Fin:</label>
                    <input
                        type="month"
                        name="fechaFin"
                        onChange={formik.handleChange}
                        onBlur={formik.handleBlur}
                        value={formik.values.fechaFin}
                        className="border p-2 w-full"
                    />
                    {formik.touched.fechaFin && formik.errors.fechaFin && (
                        <div className="text-red-500">{formik.errors.fechaFin}</div>
                    )}
                </div>
                {/* Filtro por Disciplina con sugerencias */}
                <div className="relative">
                    <label className="block font-medium">Disciplina:</label>
                    <input
                        type="text"
                        name="disciplinaNombre"
                        placeholder="Escribe el nombre de la disciplina..."
                        onChange={(e) => {
                            formik.handleChange(e);
                            setDisciplinaBusqueda(e.target.value);
                        }}
                        value={formik.values.disciplinaNombre}
                        className="border p-2 w-full"
                    />
                    {sugerenciasDisciplinas.length > 0 && (
                        <ul className="absolute bg-white border mt-1 w-full z-10">
                            {sugerenciasDisciplinas.map((disciplina: any) => (
                                <li
                                    key={disciplina.id}
                                    onClick={() => {
                                        formik.setFieldValue("disciplinaNombre", disciplina.nombre);
                                        setSugerenciasDisciplinas([]);
                                    }}
                                    className="p-2 hover:bg-gray-100 cursor-pointer"
                                >
                                    {disciplina.nombre}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
                {/* Filtro por Profesor con sugerencias */}
                <div className="relative">
                    <label className="block font-medium">Profesor:</label>
                    <input
                        type="text"
                        name="profesorNombre"
                        placeholder="Escribe el nombre del profesor..."
                        onChange={(e) => {
                            formik.handleChange(e);
                            setProfesorBusqueda(e.target.value);
                        }}
                        value={formik.values.profesorNombre}
                        className="border p-2 w-full"
                    />
                    {sugerenciasProfesores.length > 0 && (
                        <ul className="absolute bg-white border mt-1 w-full z-10">
                            {sugerenciasProfesores.map((profesor: any) => (
                                <li
                                    key={profesor.id}
                                    onClick={() => {
                                        formik.setFieldValue("profesorNombre", `${profesor.nombre} ${profesor.apellido}`);
                                        setSugerenciasProfesores([]);
                                    }}
                                    className="p-2 hover:bg-gray-100 cursor-pointer"
                                >
                                    {profesor.nombre} {profesor.apellido}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
                <div className="flex items-end">
                    <button
                        type="submit"
                        className="bg-blue-500 text-white p-2 rounded"
                        disabled={loading}
                    >
                        {loading ? "Buscando..." : "Buscar"}
                    </button>
                </div>
            </form>
            {error && <div className="text-red-500 mb-4">{error}</div>}
            <div className="overflow-x-auto">
                <Tabla
                    encabezados={[
                        "Codigo Mensualidad",
                        "Alumno",
                        "Cuota",
                        "Importe",
                        "Bonificacion",
                        "Total",
                        "Recargo",
                        "Estado",
                        "Disciplina"
                    ]}
                    datos={resultados}
                    extraRender={(item) => [
                        item.mensualidadId,
                        item.alumnoNombre,
                        item.cuota,
                        item.importe,
                        item.bonificacion,
                        item.total,
                        item.recargo,
                        item.estado,
                        item.disciplina
                    ]}
                />
            </div>
        </div>
    );
};

export default ReporteDetallePago;
