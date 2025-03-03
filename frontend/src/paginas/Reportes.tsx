import { useEffect, useState } from "react";
import Tabla from "../componentes/comunes/Tabla";
import Boton from "../componentes/comunes/Boton";
import api from "../api/axiosConfig";
import { saveAs } from "file-saver";
import { Bar } from "react-chartjs-2";
import Pagination from "../componentes/ui/Pagination";

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
  // otros campos como totalElements, etc., segun el backend
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
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);

  // Carga de disciplinas y alumnos al montar el componente
  useEffect(() => {
    const fetchDisciplinas = async () => {
      try {
        const response = await api.get<Disciplina[]>("/disciplinas");
        setDisciplinas(response.data);
      } catch (error) {
        console.error("Error al cargar disciplinas:", error);
      }
    };

    const fetchAlumnos = async () => {
      try {
        const response = await api.get<Alumno[]>("/alumnos");
        setAlumnos(response.data);
      } catch (error) {
        console.error("Error al cargar alumnos:", error);
      }
    };

    fetchDisciplinas();
    fetchAlumnos();
  }, []);

  // Funcion para obtener los datos del reporte (con paginacion)
  const obtenerDatosReporte = async (pagina = 0) => {
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
        console.error("Tipo de reporte no valido");
        setLoading(false);
        return;
    }

    try {
      const response = await api.get<PaginatedResponse<ReporteData>>(endpoint, { params });
      setDatos(response.data.content);
      setTotalPaginas(response.data.totalPages);
      setPaginaActual(pagina);
    } catch (error) {
      console.error("Error al obtener el reporte:", error);
      setErrorMsg("Ocurrio un error al obtener el reporte. Por favor, intenta de nuevo.");
    } finally {
      setLoading(false);
    }
  };

  // Funcion para exportar a Excel
  const exportarAExcel = async () => {
    try {
      const response = await api.get("/reportes/exportar/excel", {
        responseType: "blob",
      });
      saveAs(response.data, "reportes.xlsx");
    } catch (error) {
      console.error("Error exportando a Excel:", error);
    }
  };

  // Filtrado local de los datos mediante busqueda
  const datosFiltrados = datos.filter((dato) =>
    Object.values(dato)
      .join(" ")
      .toLowerCase()
      .includes(busqueda.toLowerCase())
  );

  // Datos para el grafico (por ejemplo, monto total)
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
      <h1 className="page-title">Generacion de Reportes</h1>

      <div className="space-y-4 mb-6">
        {/* Seleccion del tipo de reporte */}
        <div>
          <label htmlFor="tipoReporte" className="block text-sm font-medium text-foreground mb-1">
            Selecciona el Tipo de Reporte:
          </label>
          <select
            id="tipoReporte"
            value={tipoReporte}
            onChange={(e) => setTipoReporte(e.target.value)}
            className="form-input w-full"
          >
            <option value="">-- Seleccionar --</option>
            <option value="Recaudacion por Disciplina">Recaudacion por Disciplina</option>
            <option value="Asistencias por Alumno">Asistencias por Alumno</option>
            <option value="Asistencias por Disciplina">Asistencias por Disciplina</option>
            <option value="Asistencias por Disciplina y Alumno">Asistencias por Disciplina y Alumno</option>
          </select>
        </div>

        {/* Filtros condicionales segun el tipo de reporte */}
        {(tipoReporte.includes("Disciplina") || tipoReporte === "Recaudacion por Disciplina") && (
          <div>
            <label htmlFor="disciplina" className="block text-sm font-medium text-foreground mb-1">
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
            <label htmlFor="alumno" className="block text-sm font-medium text-foreground mb-1">
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

        {/* Campo de busqueda para filtrar localmente */}
        <div>
          <label htmlFor="busqueda" className="block text-sm font-medium text-foreground mb-1">
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

        {/* Botones de accion */}
        <div className="flex space-x-4">
          <Boton onClick={() => obtenerDatosReporte()} disabled={loading}>
            Generar Reporte
          </Boton>
          <Boton onClick={() => exportarAExcel()} disabled={loading}>
            Exportar a Excel
          </Boton>
          <Boton onClick={() => obtenerDatosReporte(paginaActual)} disabled={loading}>
            Refrescar Reporte
          </Boton>
        </div>
      </div>

      {/* Mensaje de error y loading */}
      {errorMsg && <p className="text-center text-red-600 py-4">{errorMsg}</p>}
      {loading && <p className="text-center py-4">Cargando...</p>}

      {/* Renderizado de resultados, paginacion y grafico */}
      {datosFiltrados.length > 0 && (
        <>
          <div className="page-table-container">
            <Tabla
              encabezados={["ID", "Nombre", "Disciplina", "Cantidad", "Monto Total", "Fecha"]}
              datos={datosFiltrados}
              extraRender={(fila) => [
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
            <Pagination
              currentPage={paginaActual}
              totalPages={totalPaginas}
              onPageChange={(newPage) => obtenerDatosReporte(newPage)}
            />
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
