import React, { useState, useEffect, useMemo } from "react";
import { useFormik } from "formik";
import * as Yup from "yup";
import useDebounce from "../../hooks/useDebounce";
import disciplinasApi from "../../api/disciplinasApi";
import profesoresApi from "../../api/profesoresApi";
import type { AlumnoResponse, DetallePagoResponse } from "../../types/types";
import reporteMensualidadApi, {
  ExportLiquidacionPayload,
} from "../../api/reporteMensualidadApi";
import Tabla from "../../componentes/comunes/Tabla";
import { toast } from "react-toastify";
import alumnosApi from "../../api/alumnosApi";
import NumberInputWithoutScroll from "../pagos/NumberInputWithoutScroll";

// Utilidades de fecha
const getCurrentMonth = () => {
  const now = new Date();
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
    year: "numeric",
    month: "2-digit",
  }).format(now);
};

const formatDate = (d: Date) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(d);

const transformMonthToDates = (mesStr: string) => {
  const [year, month] = mesStr.split("-").map(Number);
  const inicioDate = new Date(year, month - 1, 1);
  const finDate = new Date(year, month, 0);
  return { inicio: formatDate(inicioDate), fin: formatDate(finDate) };
};

export type RowItem = Omit<DetallePagoResponse, "alumno"> & {
  alumno: { id: number | null; nombre: string; apellido: string };
  isNew?: boolean;
};

const ReporteDetallePago: React.FC = () => {
  const [resultados, setResultados] = useState<RowItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [porcentaje, setPorcentaje] = useState(0);

  const [sugerenciasDisciplinas, setSugerenciasDisciplinas] = useState<any[]>(
    []
  );
  const [sugerenciasProfesores, setSugerenciasProfesores] = useState<any[]>([]);
  const [sugerenciasAlumnos, setSugerenciasAlumnos] = useState<any[]>([]);

  const [selectedDisciplinaId, setSelectedDisciplinaId] = useState<
    number | null
  >(null);
  const [selectedProfesorId, setSelectedProfesorId] = useState<number | null>(
    null
  );

  const [busquedaDisciplina, setBusquedaDisciplina] = useState("");
  const debouncedBusquedaDisciplina = useDebounce(busquedaDisciplina, 300);
  const [busquedaProfesor, setBusquedaProfesor] = useState("");
  const debouncedBusquedaProfesor = useDebounce(busquedaProfesor, 300);

  const [activeRowId, setActiveRowId] = useState<number | null>(null);

  const formik = useFormik({
    initialValues: {
      fechaInicio: getCurrentMonth(),
      fechaFin: getCurrentMonth(),
      disciplinaNombre: "",
      profesorNombre: "",
    },
    validationSchema: Yup.object({
      fechaInicio: Yup.string().required("El mes de inicio es obligatorio"),
      fechaFin: Yup.string().required("El mes fin es obligatorio"),
    }),
    onSubmit: async (values) => {
      setLoading(true);
      setError(null);
      try {
        const { inicio } = transformMonthToDates(values.fechaInicio);
        const { fin } = transformMonthToDates(values.fechaFin);
        const params = {
          fechaInicio: inicio,
          fechaFin: fin,
          disciplinaNombre: values.disciplinaNombre || undefined,
          profesorNombre: values.profesorNombre || undefined,
        };
        const resp = await reporteMensualidadApi.listarReporte(params);
        setResultados(resp);
      } catch (err: any) {
        toast.error("Error al obtener el reporte: " + err);
        setError("Error al cargar los datos del reporte");
      } finally {
        setLoading(false);
      }
    },
  });

  useEffect(() => {
    formik.submitForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!debouncedBusquedaDisciplina) {
      setSugerenciasDisciplinas([]);
      return;
    }
    disciplinasApi
      .buscarPorNombre(debouncedBusquedaDisciplina)
      .then(setSugerenciasDisciplinas)
      .catch(() => setSugerenciasDisciplinas([]));
  }, [debouncedBusquedaDisciplina]);

  useEffect(() => {
    if (!debouncedBusquedaProfesor) {
      setSugerenciasProfesores([]);
      return;
    }
    profesoresApi
      .buscarPorNombre(debouncedBusquedaProfesor)
      .then(setSugerenciasProfesores)
      .catch(() => setSugerenciasProfesores([]));
  }, [debouncedBusquedaProfesor]);

  useEffect(() => {
    if (selectedDisciplinaId) {
      disciplinasApi
        .obtenerAlumnosDeDisciplina(selectedDisciplinaId)
        .then(setSugerenciasAlumnos)
        .catch(() => setSugerenciasAlumnos([]));
    } else if (selectedProfesorId) {
      profesoresApi
        .findAlumnosPorProfesor(selectedProfesorId)
        .then(setSugerenciasAlumnos)
        .catch(() => setSugerenciasAlumnos([]));
    } else {
      setSugerenciasAlumnos([]);
    }
  }, [selectedDisciplinaId, selectedProfesorId]);

  const handleAddItem = () => {
    setResultados((prev) => [
      ...prev,
      {
        id: Date.now(),
        isNew: true,
        alumno: { id: null, nombre: "", apellido: "" },
        descripcionConcepto: "",
        importeInicial: 0,
        bonificacionNombre: "",
        ACobrar: 0,
        cobrado: false,
      } as RowItem,
    ]);
    if (selectedDisciplinaId) {
      disciplinasApi
        .obtenerAlumnosDeDisciplina(selectedDisciplinaId)
        .then(setSugerenciasAlumnos)
        .catch(() => setSugerenciasAlumnos([]));
    } else if (selectedProfesorId) {
      profesoresApi
        .findAlumnosPorProfesor(selectedProfesorId)
        .then(setSugerenciasAlumnos)
        .catch(() => setSugerenciasAlumnos([]));
    }
  };

  const handleCobradoChange = (id: number, checked: boolean) => {
    setResultados((prev) =>
      prev.map((it) =>
        it.id === id
          ? {
              ...it,
              cobrado: checked,
              ACobrar: checked ? it.importeInicial : 0,
            }
          : it
      )
    );
  };

  const handleDelete = (id: number) => {
    setResultados((prev) => prev.filter((it) => it.id !== id));
  };

  const resultadosOrdenados = useMemo(
    () =>
      [...resultados].sort((a, b) =>
        `${a.alumno.nombre} ${a.alumno.apellido}`.localeCompare(
          `${b.alumno.nombre} ${b.alumno.apellido}`,
          undefined,
          { sensitivity: "base" }
        )
      ),
    [resultados]
  );

  const totalACobrar = resultados.reduce(
    (sum, it) => sum + Number(it.ACobrar || 0),
    0
  );
  const montoPorcentaje = totalACobrar * (porcentaje / 100);

  const handleExport = async () => {
    setLoading(true);
    try {
      const { inicio } = transformMonthToDates(formik.values.fechaInicio);
      const { fin } = transformMonthToDates(formik.values.fechaFin);

      const detallesExport: DetallePagoResponse[] = await Promise.all(
        resultados.map(async (item) => {
          const { alumno, isNew, ...rest } = item;
          let alumnoCompleto:
            | AlumnoResponse
            | { id: number | null; nombre: string; apellido: string };

          if (isNew && alumno.id !== null) {
            alumnoCompleto = await alumnosApi.obtenerPorId(alumno.id);
          } else {
            alumnoCompleto = alumno as AlumnoResponse;
          }

          return {
            ...rest,
            alumno: alumnoCompleto as AlumnoResponse,
          } as DetallePagoResponse;
        })
      );

      const payload: ExportLiquidacionPayload = {
        fechaInicio: inicio,
        fechaFin: fin,
        disciplina: formik.values.disciplinaNombre,
        profesor: formik.values.profesorNombre,
        porcentaje,
        detalles: detallesExport,
      };

      const blob: Blob = await reporteMensualidadApi.exportarLiquidacion(
        payload
      );
      const url = URL.createObjectURL(
        new Blob([blob], { type: "application/pdf" })
      );
      const link = document.createElement("a");
      link.href = url;
      link.download = `liquidacion_${
        formik.values.profesorNombre || "profesor"
      }_${formik.values.disciplinaNombre || "disciplina"}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      toast.error("Error al exportar PDF: " + (err.message || err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold mb-4">Liquidación Profesores</h1>
      <form
        onSubmit={formik.handleSubmit}
        className="mb-4 grid grid-cols-2 gap-4 relative"
      >
        {/* Mes Inicio */}
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
        {/* Mes Fin */}
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
        {/* Disciplina */}
        <div className="relative">
          <label className="block font-medium">Disciplina:</label>
          <input
            type="text"
            name="disciplinaNombre"
            placeholder="Escribe la disciplina..."
            onChange={(e) => {
              formik.handleChange(e);
              setBusquedaDisciplina(e.target.value);
            }}
            value={formik.values.disciplinaNombre}
            className="border p-2 w-full"
          />
          {sugerenciasDisciplinas.length > 0 && (
            <ul className="absolute w-full bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerenciasDisciplinas.map((d) => (
                <li
                  key={d.id}
                  onClick={() => {
                    formik.setFieldValue("disciplinaNombre", d.nombre);
                    setSelectedDisciplinaId(d.id);
                    formik.setFieldValue(
                      "profesorNombre",
                      `${d.profesorNombre} ${d.profesorApellido}`
                    );
                    setSugerenciasDisciplinas([]);
                  }}
                  className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 cursor-pointer"
                >
                  {d.nombre} — Prof. {d.profesorNombre} {d.profesorApellido}
                </li>
              ))}
            </ul>
          )}
        </div>
        {/* Profesor */}
        <div className="relative">
          <label className="block font-medium">Profesor:</label>
          <input
            type="text"
            name="profesorNombre"
            placeholder="Escribe el profesor..."
            onChange={(e) => {
              formik.handleChange(e);
              setBusquedaProfesor(e.target.value);
            }}
            value={formik.values.profesorNombre}
            className="border p-2 w-full"
          />
          {sugerenciasProfesores.length > 0 && (
            <ul className="absolute w-full bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerenciasProfesores.map((p) => (
                <li
                  key={p.id}
                  onClick={() => {
                    formik.setFieldValue(
                      "profesorNombre",
                      `${p.nombre} ${p.apellido}`
                    );
                    setSelectedProfesorId(p.id);
                    setSugerenciasProfesores([]);
                  }}
                  className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 cursor-pointer"
                >
                  {p.nombre} {p.apellido}
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

      {/* porcentaje y agregar item */}
      <div className="mb-4">
        <label className="block font-medium">Porcentaje (%):</label>
        <div className="flex items-center">
          <NumberInputWithoutScroll
            value={porcentaje}
            onChange={(e) => setPorcentaje(Number(e.target.value))}
            className="border p-2 rounded w-24"
          />
          <button
            onClick={handleAddItem}
            className="ml-4 bg-gray-200 text-gray-800 px-3 py-1 rounded"
          >
            Agregar Item
          </button>
        </div>
      </div>

      {error && <div className="text-red-500 mb-4">{error}</div>}

      <div className="overflow-x-auto" style={{ maxHeight: "60vh" }}>
        {resultadosOrdenados.length === 0 ? (
          <div className="text-center py-4">No se encontraron resultados</div>
        ) : (
          <Tabla
            headers={[
              "Alumno",
              "Tarifa",
              "Descripción",
              "Valor Base",
              "Bonificación",
              "Monto Cobrado",
              "Cobrado",
              "Acciones",
            ]}
            data={resultadosOrdenados}
            customRender={(item) => {
              const fullName = `${item.alumno.nombre} ${item.alumno.apellido}`;
              const idx = item.descripcionConcepto.indexOf("-");
              const desc =
                idx === -1
                  ? item.descripcionConcepto
                  : item.descripcionConcepto.slice(0, idx).trim();
              const tarifa =
                idx === -1
                  ? ""
                  : item.descripcionConcepto.slice(idx + 1).trim();

              return [
                <div className="relative w-32" key="alumno">
                  <input
                    type="text"
                    value={fullName}
                    onFocus={() => setActiveRowId(item.id)}
                    onBlur={() => setTimeout(() => setActiveRowId(null), 200)}
                    onChange={(e) => {
                      const [nombre, apellido] = e.target.value.split(" ");
                      setResultados((prev) =>
                        prev.map((it) =>
                          it.id === item.id
                            ? {
                                ...it,
                                alumno: {
                                  id: it.alumno.id,
                                  nombre: nombre || "",
                                  apellido: apellido || "",
                                },
                              }
                            : it
                        )
                      );
                    }}
                    className="border p-1 w-full text-center"
                  />
                  {item.isNew &&
                    item.id === activeRowId &&
                    sugerenciasAlumnos.length > 0 && (
                      <ul className="absolute w-full bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg max-h-40 overflow-auto">
                        {sugerenciasAlumnos.map((a) => (
                          <li
                            key={a.id}
                            onClick={() => {
                              setResultados((prev) =>
                                prev.map((it) =>
                                  it.id === item.id
                                    ? {
                                        ...it,
                                        alumno: {
                                          id: a.id,
                                          nombre: a.nombre,
                                          apellido: a.apellido,
                                        },
                                      }
                                    : it
                                )
                              );
                              setSugerenciasAlumnos([]);
                            }}
                            className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 cursor-pointer"
                          >
                            {a.nombre} {a.apellido}
                          </li>
                        ))}
                      </ul>
                    )}
                </div>,
                <input
                  key="tarifa"
                  type="text"
                  value={tarifa}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? {
                              ...it,
                              descripcionConcepto: `${desc} - ${e.target.value}`,
                            }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-24 text-center"
                />,
                <input
                  key="desc"
                  type="text"
                  value={desc}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? {
                              ...it,
                              descripcionConcepto: `${e.target.value}${
                                tarifa ? ` - ${tarifa}` : ""
                              }`,
                            }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-32 text-center"
                />,
                <NumberInputWithoutScroll
                  key="importe"
                  value={item.importeInicial}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? { ...it, importeInicial: Number(e.target.value) }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-24 text-center"
                />,
                <input
                  key="bonif"
                  type="text"
                  value={item.bonificacionNombre || ""}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? { ...it, bonificacionNombre: e.target.value }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-32 text-center"
                />,
                <NumberInputWithoutScroll
                  key="acobrar"
                  value={item.ACobrar}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? { ...it, ACobrar: Number(e.target.value) }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-24 text-center"
                />,
                <input
                  key="cobrado"
                  type="checkbox"
                  checked={item.cobrado || false}
                  onChange={(e) =>
                    handleCobradoChange(item.id, e.target.checked)
                  }
                  className="border p-1"
                />,
                <button
                  key="eliminar"
                  onClick={() => handleDelete(item.id)}
                  className="text-red-500 p-1"
                >
                  Eliminar
                </button>,
              ];
            }}
          />
        )}
      </div>

      <div className="mt-4 text-center">
        <p>Total cobrado: $ {totalACobrar.toLocaleString()}</p>
        <p>
          Liquidación neta ({porcentaje}%): $ {montoPorcentaje.toLocaleString()}
        </p>
      </div>

      <button
        onClick={handleExport}
        className="mt-2 bg-green-600 text-white px-4 py-2 rounded"
        disabled={loading || !resultados.length}
      >
        {loading ? "Generando PDF..." : "Exportar PDF"}
      </button>
    </div>
  );
};

export default ReporteDetallePago;
