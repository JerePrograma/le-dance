// ReporteDetallePago.tsx
import React, { useState, useEffect } from "react";
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

// Función auxiliar para obtener el mes actual en formato "YYYY-MM"
// considerando la zona horaria "America/Argentina/Buenos_Aires"
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

  // Estados para sugerencias de disciplina y profesor
  const [sugerenciasDisciplinas, setSugerenciasDisciplinas] = useState<any[]>(
    []
  );
  const [sugerenciasProfesores, setSugerenciasProfesores] = useState<any[]>([]);
  const [disciplinaBusqueda, setDisciplinaBusqueda] = useState<string>("");
  const debouncedDisciplinaBusqueda = useDebounce(disciplinaBusqueda, 300);
  const [profesorBusqueda, setProfesorBusqueda] = useState<string>("");
  const debouncedProfesorBusqueda = useDebounce(profesorBusqueda, 300);

  // Configuración de Formik para los filtros
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
        // 1) Transformar "YYYY-MM" a fechas completas
        const transformarMesAFechas = (
          mesStr: string
        ): { inicio: string; fin: string } => {
          const [year, month] = mesStr.split("-").map(Number);
          const inicio = new Date(year, month - 1, 1);
          const fin = new Date(year, month, 0);
          const formatear = (d: Date) =>
            new Intl.DateTimeFormat("en-CA", {
              timeZone: "America/Argentina/Buenos_Aires",
              year: "numeric",
              month: "2-digit",
              day: "2-digit",
            }).format(d);
          return { inicio: formatear(inicio), fin: formatear(fin) };
        };

        const { inicio: inicioFecha } = transformarMesAFechas(
          values.fechaInicio
        );
        const { fin: finFecha } = transformarMesAFechas(values.fechaFin);

        // 2) Preparar parámetros y llamar al backend
        const params = {
          fechaInicio: inicioFecha,
          fechaFin: finFecha,
          disciplinaNombre: values.disciplinaNombre || undefined,
          profesorNombre: values.profesorNombre || undefined,
        };
        console.log(
          "Llamando a /api/reportes/mensualidades/buscar con parámetros:",
          params
        );
        const response: DetallePagoResponse[] =
          await reporteMensualidadApi.listarReporte(params);
        console.log("Respuesta recibida (antes de ordenar):", response);

        // 3) Ordenar alfabéticamente por descripcionConcepto
        const ordenado = [...response].sort((a, b) =>
          a.descripcionConcepto.localeCompare(
            b.descripcionConcepto,
            undefined,
            { sensitivity: "base" }
          )
        );
        console.log("Resultados ordenados:", ordenado);

        setResultados(ordenado);
      } catch (err: any) {
        toast.error("Error al obtener el reporte: " + err);
        setError("Error al cargar los datos del reporte");
      } finally {
        setLoading(false);
      }
    },
  });

  // Auto-submit al montar el componente
  useEffect(() => {
    formik.submitForm();
  }, []);

  // Sugerencias de disciplinas
  useEffect(() => {
    const buscarSugerenciasDisciplinas = async () => {
      if (debouncedDisciplinaBusqueda) {
        try {
          const sugerencias = await disciplinasApi.buscarPorNombre(
            debouncedDisciplinaBusqueda
          );
          console.log("Sugerencias de disciplinas:", sugerencias);
          setSugerenciasDisciplinas(sugerencias);
        } catch (err) {
          toast.error("Error al buscar sugerencias de disciplinas:");
          setSugerenciasDisciplinas([]);
        }
      } else {
        setSugerenciasDisciplinas([]);
      }
    };
    buscarSugerenciasDisciplinas();
  }, [debouncedDisciplinaBusqueda]);

  // Sugerencias de profesores
  useEffect(() => {
    const buscarSugerenciasProfesores = async () => {
      if (debouncedProfesorBusqueda) {
        try {
          const sugerencias = await profesoresApi.buscarPorNombre(
            debouncedProfesorBusqueda
          );
          console.log("Sugerencias de profesores:", sugerencias);
          setSugerenciasProfesores(sugerencias);
        } catch (err) {
          toast.error("Error al buscar sugerencias de profesores:");
          setSugerenciasProfesores([]);
        }
      } else {
        setSugerenciasProfesores([]);
      }
    };
    buscarSugerenciasProfesores();
  }, [debouncedProfesorBusqueda]);

  // Función para actualizar campos dinámicos de cada fila
  const actualizarCampo = (
    id: number,
    campo: keyof DetallePagoResponse,
    valor: any
  ) => {
    setResultados((prev) =>
      prev.map((it) => (it.id === id ? { ...it, [campo]: valor } : it))
    );
  };

  const totalACobrar = resultados.reduce(
    (sum, item) => sum + Number(item.ACobrar || 0),
    0
  );
  const montoPorcentaje = totalACobrar * (porcentaje / 100);

  // --- Función para exportar PDF con fechas completas ---
  const handleExport = async () => {
    try {
      setLoading(true);

      // 1) Transformar "YYYY-MM" a fechas completas "YYYY-MM-DD"
      const transformarMesAFechas = (
        mesStr: string
      ): { inicio: string; fin: string } => {
        const [year, month] = mesStr.split("-").map(Number);
        const inicio = new Date(year, month - 1, 1);
        const fin = new Date(year, month, 0);
        const formatear = (d: Date) =>
          new Intl.DateTimeFormat("en-CA", {
            timeZone: "America/Argentina/Buenos_Aires",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
          }).format(d);
        return { inicio: formatear(inicio), fin: formatear(fin) };
      };

      const { inicio: inicioFecha } = transformarMesAFechas(
        formik.values.fechaInicio
      );
      const { fin: finFecha } = transformarMesAFechas(formik.values.fechaFin);

      // 2) Armar payload con fechas completas
      const payload = {
        fechaInicio: inicioFecha, // ej. "2025-04-01"
        fechaFin: finFecha, // ej. "2025-04-30"
        disciplina: formik.values.disciplinaNombre,
        profesor: formik.values.profesorNombre,
        porcentaje,
        detalles: resultados,
      };

      // 3) Llamada al backend y forzar descarga
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
      toast.error("Error al exportar PDF: " + (err?.message || err));
    } finally {
      setLoading(false);
    }
  };
  
  const resultadosOrdenados = React.useMemo(
    () =>
      [...resultados].sort((a, b) =>
        a.descripcionConcepto.localeCompare(b.descripcionConcepto, undefined, {
          sensitivity: "base",
        })
      ),
    [resultados]
  );

  /**
   * Actualiza dinámicamente la descripción o tarifa dentro de resultados.
   * @param id        El id del DetallePagoResponse a actualizar.
   * @param campo     "descripcion" o "tarifa".
   * @param valor     El nuevo valor para ese campo.
   */
  const handleCampoChange = (
    id: number,
    campo: "descripcion" | "tarifa",
    valor: string
  ) => {
    setResultados((prev) =>
      prev.map((item) => {
        if (item.id !== id) return item;

        // Separa la cadena actual en descripción y tarifa
        const idx = item.descripcionConcepto.indexOf(" - ");
        const currentDesc =
          idx >= 0
            ? item.descripcionConcepto.substring(0, idx).trim()
            : item.descripcionConcepto.trim();
        const currentTarifa =
          idx >= 0 ? item.descripcionConcepto.substring(idx + 3).trim() : "";

        // Sustituye el campo que corresponde
        const nuevaDesc = campo === "descripcion" ? valor : currentDesc;
        const nuevaTar = campo === "tarifa" ? valor : currentTarifa;

        // Reconstruye la cadena
        const nuevaCadena =
          nuevaTar !== "" ? `${nuevaDesc} - ${nuevaTar}` : nuevaDesc;

        return {
          ...item,
          descripcionConcepto: nuevaCadena,
        };
      })
    );
  };

  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold mb-4">Liquidación Profesores</h1>
      <form
        onSubmit={formik.handleSubmit}
        className="mb-4 grid grid-cols-2 gap-4 relative"
      >
        {/* Filtros de Fecha */}
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
        {/* Filtro de Disciplina */}
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
            <ul className="absolute w-full bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerenciasDisciplinas.map((disc: any) => (
                <li
                  key={disc.id}
                  onClick={() => {
                    formik.setFieldValue("disciplinaNombre", disc.nombre);
                    const profFullName = `${disc.profesorNombre} ${disc.profesorApellido}`;
                    formik.setFieldValue("profesorNombre", profFullName);
                    setSugerenciasDisciplinas([]);
                  }}
                  className="bg-slate-200 dark:bg-slate-600 hover:bg-gray-200 dark:hover:bg-gray-700 p-1"
                >
                  {disc.nombre} — Prof. {disc.profesorNombre}{" "}
                  {disc.profesorApellido}
                </li>
              ))}
            </ul>
          )}
        </div>
        {/* Filtro de Profesor */}
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
            <ul className="absolute bg-gray-100 dark:bg-gray-800 border border-gray-300 dark:border-gray-700 mt-1 z-10 rounded-md shadow-lg">
              {sugerenciasProfesores.map((profesor: any) => (
                <li
                  key={profesor.id}
                  onClick={() => {
                    formik.setFieldValue(
                      "profesorNombre",
                      `${profesor.nombre} ${profesor.apellido}`
                    );
                    setSugerenciasProfesores([]);
                  }}
                  className="bg-slate-200 dark:bg-slate-600 hover:bg-gray-200 dark:hover:bg-gray-700 p-1"
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

      {/* Porcentaje dinámico */}
      <div className="mb-4">
        <label className="block font-medium">Porcentaje (%):</label>
        <input
          type="number"
          value={porcentaje}
          onChange={(e) => setPorcentaje(Number(e.target.value))}
          className="border p-2 rounded w-auto"
        />
      </div>

      {error && <div className="text-red-500 mb-4">{error}</div>}
      <div className="overflow-x-auto">
        {resultados.length === 0 ? (
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
            ]}
            data={resultadosOrdenados}
            customRender={(item: DetallePagoResponse) => {
              const index = item.descripcionConcepto.indexOf("-");
              const descripcion =
                index === -1
                  ? item.descripcionConcepto
                  : item.descripcionConcepto.substring(0, index).trim();
              const tarifa =
                index === -1
                  ? ""
                  : item.descripcionConcepto.substring(index + 1).trim();
              return [
                <input
                  type="text"
                  value={item.alumno.nombre + " " + item.alumno.apellido}
                  onChange={(e) =>
                    setResultados((prev) =>
                      prev.map((it) =>
                        it.id === item.id
                          ? { ...it, alumnoDisplay: e.target.value }
                          : it
                      )
                    )
                  }
                  className="border p-1 w-auto text-center"
                />,
                // Tarifa
                <input
                  type="text"
                  value={tarifa}
                  onChange={(e) =>
                    handleCampoChange(item.id, "tarifa", e.target.value)
                  }
                  className="border p-1 w-auto text-center"
                />,
                // Descripción
                <input
                  type="text"
                  value={descripcion}
                  onChange={(e) =>
                    handleCampoChange(item.id, "descripcion", e.target.value)
                  }
                  className="border p-1 w-auto text-center"
                />,
                // Valor Base (importeInicial)
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
                // Bonificación
                <input
                  type="text"
                  value={item.bonificacionNombre}
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
                // Monto Cobrado (A Cobrar)
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
                // Cobrado (checkbox)
                <input
                  type="checkbox"
                  checked={item.cobrado || false}
                  onChange={(e) =>
                    actualizarCampo(item.id, "cobrado", e.target.checked)
                  }
                  className="border p-1 w-auto text-center"
                />,
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
        className="bg-green-600 text-white px-4 py-2 rounded"
        disabled={loading || resultados.length === 0}
      >
        {loading ? "Generando PDF..." : "Exportar PDF"}
      </button>
    </div>
  );
};

export default ReporteDetallePago;
