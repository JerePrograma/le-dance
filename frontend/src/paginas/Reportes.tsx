import { useEffect, useState } from "react";
import Tabla from "../componentes/comunes/Tabla";
import Boton from "../componentes/comunes/Boton";
import api from "../utilidades/axiosConfig";

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

const Reportes = () => {
  const [tipoReporte, setTipoReporte] = useState("");
  const [datos, setDatos] = useState<ReporteData[]>([]);
  const [disciplinas, setDisciplinas] = useState<Disciplina[]>([]);
  const [alumnos, setAlumnos] = useState<Alumno[]>([]);
  const [filtroDisciplina, setFiltroDisciplina] = useState("");
  const [filtroAlumno, setFiltroAlumno] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchDisciplinas = async () => {
      try {
        const response = await api.get<Disciplina[]>("/api/disciplinas");
        setDisciplinas(response.data);
      } catch (error) {
        console.error("Error al cargar disciplinas:", error);
      }
    };

    const fetchAlumnos = async () => {
      try {
        const response = await api.get<Alumno[]>("/api/alumnos");
        setAlumnos(response.data);
      } catch (error) {
        console.error("Error al cargar alumnos:", error);
      }
    };

    fetchDisciplinas();
    fetchAlumnos();
  }, []);

  const obtenerDatosReporte = async () => {
    if (!tipoReporte) return;

    setLoading(true);
    let endpoint = "";
    let params: Record<string, string> = {};

    switch (tipoReporte) {
      case "Recaudación por Disciplina":
        endpoint = "/api/reportes/recaudacion-disciplina";
        break;
      case "Asistencias por Alumno":
        endpoint = "/api/reportes/asistencias-alumno";
        params = { alumnoId: filtroAlumno };
        break;
      case "Asistencias por Disciplina":
        endpoint = "/api/reportes/asistencias-disciplina";
        params = { disciplinaId: filtroDisciplina };
        break;
      case "Asistencias por Disciplina y Alumno":
        endpoint = "/api/reportes/asistencias-disciplina-alumno";
        params = { disciplinaId: filtroDisciplina, alumnoId: filtroAlumno };
        break;
      default:
        console.error("Tipo de reporte no válido");
        return;
    }

    try {
      const response = await api.get(endpoint, { params });
      setDatos(response.data);
    } catch (error) {
      console.error("Error al obtener el reporte:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container @container">
      <h1 className="page-title">Generación de Reportes</h1>

      <div className="space-y-4 mb-6">
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
            <option value="Recaudación por Disciplina">
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

        {tipoReporte.includes("Disciplina") && (
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

        <Boton onClick={obtenerDatosReporte} disabled={loading}>
          Generar Reporte
        </Boton>
      </div>

      {loading && <p className="text-center py-4">Cargando...</p>}

      {datos.length > 0 && (
        <div className="page-table-container">
          <Tabla
            encabezados={[
              "ID",
              "Nombre",
              "Disciplina",
              "Cantidad",
              "Monto Total",
              "Fecha",
            ]}
            datos={datos}
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
      )}
    </div>
  );
};

export default Reportes;
