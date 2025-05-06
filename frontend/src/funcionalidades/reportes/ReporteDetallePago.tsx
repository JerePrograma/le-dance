import React, { useState, useEffect, useMemo } from "react";
import { useFormik } from "formik";
import * as Yup from "yup";
import useDebounce from "../../hooks/useDebounce";
import disciplinasApi from "../../api/disciplinasApi";
import profesoresApi from "../../api/profesoresApi";
import type { DetallePagoResponse } from "../../types/types";
import reporteMensualidadApi from "../../api/reporteMensualidadApi";
import Tabla from "../../componentes/comunes/Tabla";
import { toast } from "react-toastify";

interface FiltrosBusqueda {
  fechaInicio: string;
  fechaFin: string;
  disciplinaNombre: string;
  profesorNombre: string;
}

const getCurrentMonth = () => {
  const now = new Date();
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Argentina/Buenos_Aires",
    year: "numeric",
    month: "2-digit",
  }).format(now);
};

const ReporteDetallePago: React.FC = () => {
  const [resultados, setResultados] = useState<DetallePagoResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [porcentaje, setPorcentaje] = useState<number>(0);

  const [sugerenciasDisciplinas, setSugerenciasDisciplinas] = useState<any[]>(
    []
  );
  const [sugerenciasProfesores, setSugerenciasProfesores] = useState<any[]>([]);
  const [disciplinaBusqueda, setDisciplinaBusqueda] = useState<string>("");
  const debouncedDisciplinaBusqueda = useDebounce(disciplinaBusqueda, 300);
  const [profesorBusqueda, setProfesorBusqueda] = useState<string>("");
  const debouncedProfesorBusqueda = useDebounce(profesorBusqueda, 300);

  const formik = useFormik<FiltrosBusqueda>({
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
        const transformarMesAFechas = (mesStr: string) => {
          const [year, month] = mesStr.split("-").map(Number);
          const inicioDate = new Date(year, month - 1, 1);
          const finDate = new Date(year, month, 0);
          const formatear = (d: Date) =>
            new Intl.DateTimeFormat("en-CA", {
              timeZone: "America/Argentina/Buenos_Aires",
              year: "numeric",
              month: "2-digit",
              day: "2-digit",
            }).format(d);
          return { inicio: formatear(inicioDate), fin: formatear(finDate) };
        };
        const { inicio } = transformarMesAFechas(values.fechaInicio);
        const { fin } = transformarMesAFechas(values.fechaFin);
        const params = {
          fechaInicio: inicio,
          fechaFin: fin,
          disciplinaNombre: values.disciplinaNombre || undefined,
          profesorNombre: values.profesorNombre || undefined,
        };
        const response = await reporteMensualidadApi.listarReporte(params);
        setResultados(response);
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
  }, []);
  useEffect(() => {
    async function fetch() {
      if (debouncedDisciplinaBusqueda) {
        try {
          setSugerenciasDisciplinas(
            await disciplinasApi.buscarPorNombre(debouncedDisciplinaBusqueda)
          );
        } catch {
          setSugerenciasDisciplinas([]);
        }
      } else setSugerenciasDisciplinas([]);
    }
    fetch();
  }, [debouncedDisciplinaBusqueda]);
  useEffect(() => {
    async function fetch() {
      if (debouncedProfesorBusqueda) {
        try {
          setSugerenciasProfesores(
            await profesoresApi.buscarPorNombre(debouncedProfesorBusqueda)
          );
        } catch {
          setSugerenciasProfesores([]);
        }
      } else setSugerenciasProfesores([]);
    }
    fetch();
  }, [debouncedProfesorBusqueda]);

  const handleCobradoChange = (id: number, checked: boolean) => {
    setResultados((prev) =>
      prev.map((item) =>
        item.id === id
          ? {
              ...item,
              cobrado: checked,
              ACobrar: checked ? item.importeInicial : 0,
            }
          : item
      )
    );
  };
  const handleDelete = (id: number) =>
    setResultados((prev) => prev.filter((item) => item.id !== id));

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
    (sum, item) => sum + Number(item.ACobrar || 0),
    0
  );
  const montoPorcentaje = totalACobrar * (porcentaje / 100);

  const handleExport = async () => {
    setLoading(true);
    try {
      const transformarMesAFechas = (mesStr: string) => {
        const [year, month] = mesStr.split("-").map(Number);
        const inicioDate = new Date(year, month - 1, 1);
        const finDate = new Date(year, month, 0);
        const formatear = (d: Date) =>
          new Intl.DateTimeFormat("en-CA", {
            timeZone: "America/Argentina/Buenos_Aires",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
          }).format(d);
        return { inicio: formatear(inicioDate), fin: formatear(finDate) };
      };
      const { inicio } = transformarMesAFechas(formik.values.fechaInicio);
      const { fin } = transformarMesAFechas(formik.values.fechaFin);
      const payload = {
        fechaInicio: inicio,
        fechaFin: fin,
        disciplina: formik.values.disciplinaNombre,
        profesor: formik.values.profesorNombre,
        porcentaje,
        detalles: resultados,
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
              setDisciplinaBusqueda(e.target.value);
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
              setProfesorBusqueda(e.target.value);
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
      <div className="mb-4">
        <label className="block font-medium">Porcentaje (%):</label>
        <input
          type="number"
          value={porcentaje}
          onChange={(e) => setPorcentaje(Number(e.target.value))}
          className="border p-2 rounded w-24"
        />
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
              const descripcion =
                idx === -1
                  ? item.descripcionConcepto
                  : item.descripcionConcepto.substring(0, idx).trim();
              const tarifa =
                idx === -1
                  ? ""
                  : item.descripcionConcepto.substring(idx + 1).trim();
              return [
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => {
                    const [nombre, apellido] = e.target.value.split(" ");
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? {
                              ...it,
                              alumno: {
                                ...it.alumno,
                                nombre: nombre || it.alumno.nombre,
                                apellido: apellido || it.alumno.apellido,
                              },
                            }
                          : it
                      )
                    );
                  }}
                  className="border p-1 w-auto text-center"
                />,
                <input
                  type="text"
                  value={tarifa}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? {
                              ...it,
                              descripcionConcepto: `${descripcion} - ${e.target.value}`,
                            }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-auto text-center"
                />,
                <input
                  type="text"
                  value={descripcion}
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
                  className="border p-1 w-auto text-center"
                />,
                <input
                  type="number"
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
                  className="border p-1 w-auto text-center"
                />,
                <input
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
                  className="border p-1 w-auto text-center"
                />,
                <input
                  type="number"
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
                  className="border p-1 w-auto text-center"
                />,
                <input
                  type="checkbox"
                  checked={item.cobrado || false}
                  onChange={(e) =>
                    handleCobradoChange(item.id, e.target.checked)
                  }
                  className="border p-1"
                />,
                <button
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
