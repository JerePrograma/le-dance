"use client";

import { useEffect, useState, useCallback, useMemo } from "react";
import Tabla from "../componentes/comunes/Tabla";
import Boton from "../componentes/comunes/Boton";
import api from "../api/axiosConfig";
import { saveAs } from "file-saver";
import { Bar } from "react-chartjs-2";
import { toast } from "react-toastify";
import ListaConInfiniteScroll from "../componentes/comunes/ListaConInfiniteScroll";

// Interfaces de datos
interface ReporteData {
  id: number;
  nombre?: string;
  disciplina?: string;
  cantidad?: number;
  montoTotal?: number;
  fecha?: string;
}

interface Disciplina {
  id: number;
  nombre: string;
}

interface Alumno {
  id: number;
  nombre: string;
  apellido: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  // otros campos si son necesarios
}

const Reportes = () => {
  const [tipoReporte, setTipoReporte] = useState("");
  const [datos, setDatos] = useState<ReporteData[]>([]);
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const [alumnos, setAlumnos] = useState<Alumno[]>([]);
  const [filtroDisciplina, setFiltroDisciplina] = useState("");
  const [filtroAlumno, setFiltroAlumno] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [busqueda, setBusqueda] = useState("");
  // Estado para llevar el control de la página actual y total de páginas
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);

  // Carga de disciplinas y alumnos al montar el componente
  useEffect(() => {
    const fetchDisciplinas = async () => {
      try {
        const response = await api.get<Disciplina[]>("/disciplinas");
        setDisciplinas(response.data);
      } catch (error) {
        toast.error("Error al cargar disciplinas:");
      }
    };
    const fetchAlumnos = async () => {
      try {
        const response = await api.get<Alumno[]>("/alumnos");
        setAlumnos(response.data);
      } catch (error) {
        toast.error("Error al cargar alumnos:");
      }
    };
    fetchDisciplinas();
    fetchAlumnos();
  }, []);

  /**
   * Función para cargar los datos del reporte.
   * Si reset es true se reemplaza la lista (por ejemplo, al generar o refrescar el reporte);
   * de lo contrario, se concatenan los datos.
   */
  const cargarReportes = useCallback(
    async (pagina = 0, reset: boolean = false) => {
      if (!tipoReporte) return;
      setLoading(true);
      setErrorMsg("");
      let endpoint = "";
      let params: Record<string, any> = { page: pagina };
      switch (tipoReporte) {
        case "Recaudacion por Disciplina":
          endpoint = "/reportes/recaudacion-disciplina";
          if (filtroDisciplina) {
            params.disciplinaId = filtroDisciplina;
          }
          break;
        case "Asistencias por Alumno":
          endpoint = "/reportes/asistencias-alumno";
          if (filtroAlumno) {
            params.alumnoId = filtroAlumno;
          }
          break;
        case "Asistencias por Disciplina":
          endpoint = "/reportes/asistencias-disciplina";
          if (filtroDisciplina) {
            params.disciplinaId = filtroDisciplina;
          }
          break;
        case "Asistencias por Disciplina y Alumno":
          endpoint = "/reportes/asistencias-disciplina-alumno";
          if (filtroDisciplina) {
            params.disciplinaId = filtroDisciplina;
          }
          if (filtroAlumno) {
            params.alumnoId = filtroAlumno;
          }
          break;
        default:
          toast.error("Tipo de reporte no válido");
          setLoading(false);
          return;
      }
      try {
        const response = await api.get<PaginatedResponse<ReporteData>>(
          endpoint,
          { params }
        );
        if (reset) {
          setDatos(response.data.content);
        } else {
          setDatos((prev) => [...prev, ...response.data.content]);
        }
        setTotalPaginas(response.data.totalPages);
        setPaginaActual(pagina);
      } catch (error) {
        toast.error("Error al obtener el reporte:");
        setErrorMsg(
          "Ocurrió un error al obtener el reporte. Por favor, intenta de nuevo."
        );
      } finally {
        setLoading(false);
      }
    },
    [tipoReporte, filtroDisciplina, filtroAlumno]
  );

  // Función para exportar a Excel
  const exportarAExcel = async () => {
    try {
      const response = await api.get("/reportes/exportar/excel", {
        responseType: "blob",
      });
      saveAs(response.data, "reportes.xlsx");
    } catch (error) {
      toast.error("Error exportando a Excel:");
    }
  };

  // Filtrado local de los datos mediante el campo de búsqueda
  const datosFiltrados = useMemo(
    () =>
      datos.filter((dato) =>
        Object.values(dato)
          .join(" ")
          .toLowerCase()
          .includes(busqueda.toLowerCase())
      ),
    [datos, busqueda]
  );

  // Datos para el gráfico (por ejemplo, monto total)
  const chartData = {
    labels: datos.map((d) => d.nombre || "Sin nombre"),
    datasets: [
      {
        label: "Monto Total",
        data: datos.map((d) => d.montoTotal || 0),
        backgroundColor: "rgba(75, 192, 192, 0.6)",
      },
    ],
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Generación de Reportes</h1>

      <div className="space-y-4 mb-6">
        {/* Selección del tipo de reporte */}
        <div>
          <label
            htmlFor="tipoReporte"
            className="block text-sm font-medium text-foreground mb-1"
          >
            Selecciona el Tipo de Reporte:
          </label>
          <select
            id="tipoReporte"
            value={tipoReporte}
            onChange={(e) => setTipoReporte(e.target.value)}
            className="form-input w-full"
          >
            <option value="">-- Seleccionar --</option>
            <option value="Recaudacion por Disciplina">
              Recaudación por Disciplina
            </option>
            <option value="Asistencias por Alumno">
              Asistencias por Alumno
            </option>
            <option value="Asistencias por Disciplina">
              Asistencias por Disciplina
            </option>
            <option value="Asistencias por Disciplina y Alumno">
              Asistencias por Disciplina y Alumno
            </option>
          </select>
        </div>

        {/* Filtros condicionales según el tipo de reporte */}
        {(tipoReporte.includes("Disciplina") ||
          tipoReporte === "Recaudacion por Disciplina") && (
          <div>
            <label
              htmlFor="disciplina"
              className="block text-sm font-medium text-foreground mb-1"
            >
              Selecciona la Disciplina:
            </label>
            <select
              id="disciplina"
              value={filtroDisciplina}
              onChange={(e) => setFiltroDisciplina(e.target.value)}
              className="form-input w-full"
            >
              <option value="">-- Seleccionar --</option>
              {disciplinas.map((disc) => (
                <option key={disc.id} value={disc.id}>
                  {disc.nombre}
                </option>
              ))}
            </select>
          </div>
        )}

        {tipoReporte.includes("Alumno") && (
          <div>
            <label
              htmlFor="alumno"
              className="block text-sm font-medium text-foreground mb-1"
            >
              Selecciona el Alumno:
            </label>
            <select
              id="alumno"
              value={filtroAlumno}
              onChange={(e) => setFiltroAlumno(e.target.value)}
              className="form-input w-full"
            >
              <option value="">-- Seleccionar --</option>
              {alumnos.map((alumno) => (
                <option key={alumno.id} value={alumno.id}>
                  {alumno.nombre} {alumno.apellido}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Campo de búsqueda para filtrar localmente */}
        <div>
          <label
            htmlFor="busqueda"
            className="block text-sm font-medium text-foreground mb-1"
          >
            Buscar en Reporte:
          </label>
          <input
            id="busqueda"
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            className="form-input w-full"
            placeholder="Buscar..."
          />
        </div>

        {/* Botones de acción */}
        <div className="flex space-x-4">
          <Boton onClick={() => cargarReportes(0, true)} disabled={loading}>
            Generar Reporte
          </Boton>
          <Boton onClick={exportarAExcel} disabled={loading}>
            Exportar a Excel
          </Boton>
          <Boton
            onClick={() => cargarReportes(paginaActual, true)}
            disabled={loading}
          >
            Refrescar Reporte
          </Boton>
        </div>
      </div>

      {/* Mensaje de error y loading */}
      {errorMsg && <p className="text-center text-red-600 py-4">{errorMsg}</p>}
      {loading && <p className="text-center py-4">Cargando...</p>}

      {/* Renderizado de resultados, tabla, infinite scroll y gráfico */}
      {datosFiltrados.length > 0 && (
        <>
          <div className="page-table-container">
            <Tabla
              headers={[
                "ID",
                "Nombre",
                "Disciplina",
                "Cantidad",
                "Monto Total",
                "Fecha",
              ]}
              data={datosFiltrados}
              customRender={(fila) => [
                fila.id,
                fila.nombre || "-",
                fila.disciplina || "-",
                fila.cantidad || "-",
                fila.montoTotal ? `$${fila.montoTotal}` : "-",
                fila.fecha || "-",
              ]}
            />
          </div>
          <div className="mt-4">
            <ListaConInfiniteScroll
              onLoadMore={() => {
                if (paginaActual + 1 < totalPaginas) {
                  cargarReportes(paginaActual + 1);
                }
              }}
              hasMore={paginaActual + 1 < totalPaginas}
              loading={loading}
              className="justify-center w-full"
              children={undefined}
            ></ListaConInfiniteScroll>
          </div>
          <div className="mt-6">
            <Bar data={chartData} />
          </div>
        </>
      )}
    </div>
  );
};

export default Reportes;
