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

  // Sugerencias
  const [sugerenciasDisciplinas, setSugerenciasDisciplinas] = useState<any[]>(
    []
  );
  const [sugerenciasProfesores, setSugerenciasProfesores] = useState<any[]>([]);
  const [disciplinaBusqueda, setDisciplinaBusqueda] = useState<string>("");
  const debouncedDisciplinaBusqueda = useDebounce(disciplinaBusqueda, 300);
  const [profesorBusqueda, setProfesorBusqueda] = useState<string>("");
  const debouncedProfesorBusqueda = useDebounce(profesorBusqueda, 300);

  // Formik
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
        const transformarMesAFechas = (
          mesStr: string
        ): { inicio: string; fin: string } => {
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

  // Auto-submit al montar
  useEffect(() => {
    formik.submitForm();
  }, []);

  // Sugerencias de disciplinas
  useEffect(() => {
    const fetch = async () => {
      if (debouncedDisciplinaBusqueda) {
        try {
          const s = await disciplinasApi.buscarPorNombre(
            debouncedDisciplinaBusqueda
          );
          setSugerenciasDisciplinas(s);
        } catch {
          setSugerenciasDisciplinas([]);
        }
      } else setSugerenciasDisciplinas([]);
    };
    fetch();
  }, [debouncedDisciplinaBusqueda]);

  // Sugerencias de profesores
  useEffect(() => {
    const fetch = async () => {
      if (debouncedProfesorBusqueda) {
        try {
          const s = await profesoresApi.buscarPorNombre(
            debouncedProfesorBusqueda
          );
          setSugerenciasProfesores(s);
        } catch {
          setSugerenciasProfesores([]);
        }
      } else setSugerenciasProfesores([]);
    };
    fetch();
  }, [debouncedProfesorBusqueda]);

  // Cobrado / Eliminar
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

  // Ordenar por alumno
  const resultadosOrdenados = useMemo(
    () =>
      [...resultados].sort((a, b) => {
        const nameA = `${a.alumno.nombre} ${a.alumno.apellido}`;
        const nameB = `${b.alumno.nombre} ${b.alumno.apellido}`;
        return nameA.localeCompare(nameB, undefined, { sensitivity: "base" });
      }),
    [resultados]
  );

  // Totales
  const totalACobrar = resultados.reduce(
    (sum, item) => sum + Number(item.ACobrar || 0),
    0
  );
  const montoPorcentaje = totalACobrar * (porcentaje / 100);

  // Export PDF
  const handleExport = async () => {
    setLoading(true);
    try {
      const transformarMesAFechas = (
        mesStr: string
      ): { inicio: string; fin: string } => {
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
      const url = window.URL.createObjectURL(
        new Blob([blob], { type: "application/pdf" })
      );
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        `liquidacion_${formik.values.profesorNombre || "profesor"}_${
          formik.values.disciplinaNombre || "disciplina"
        }.pdf`
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
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
              {sugerenciasDisciplinas.map((disc) => (
                <li
                  key={disc.id}
                  onClick={() => {
                    formik.setFieldValue("disciplinaNombre", disc.nombre);
                    formik.setFieldValue(
                      "profesorNombre",
                      `${disc.profesorNombre} ${disc.profesorApellido}`
                    );
                    setSugerenciasDisciplinas([]);
                  }}
                  className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 cursor-pointer"
                >
                  {disc.nombre} — Prof. {disc.profesorNombre}{" "}
                  {disc.profesorApellido}
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
              {sugerenciasProfesores.map((prof) => (
                <li
                  key={prof.id}
                  onClick={() => {
                    formik.setFieldValue(
                      "profesorNombre",
                      `${prof.nombre} ${prof.apellido}`
                    );
                    setSugerenciasProfesores([]);
                  }}
                  className="p-1 hover:bg-gray-200 dark:hover:bg-gray-700 cursor-pointer"
                >
                  {prof.nombre} {prof.apellido}
                </li>
              ))}
            </ul>
          )}
        </div>
        {/* Botón Buscar */}
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

      {/* Porcentaje */}
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
                <span className="text-center w-auto">{fullName}</span>,
                <span className="text-center w-auto">{tarifa}</span>,
                <span className="text-center w-auto">{descripcion}</span>,
                <span className="text-center w-auto">
                  {item.importeInicial}
                </span>,
                <span className="text-center w-auto">
                  {item.bonificacionNombre}
                </span>,
                <span className="text-center w-auto">{item.ACobrar}</span>,
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

      {/* Totales y Export */}
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
