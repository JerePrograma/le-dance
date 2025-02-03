import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import asistenciasApi from "../../utilidades/asistenciasApi";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Asistencia {
  id: number;
  alumno: {
    id: number;
    nombre: string;
    apellido: string;
    activo: boolean;
  };
  disciplina: {
    id: number;
    nombre: string;
  };
  fecha: string;
  presente: boolean;
  profesor?: {
    id: number;
    nombre: string;
    apellido: string;
  } | null;
}

const Asistencias = () => {
  const [asistencias, setAsistencias] = useState<Asistencia[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchAsistencias = useCallback(async () => {
    try {
      setLoading(true);
      const data = await asistenciasApi.listarAsistencias();

      // Asegurar que activo nunca sea `undefined`
      const formattedData = data.map((asistencia) => ({
        ...asistencia,
        alumno: {
          ...asistencia.alumno,
          activo: asistencia.alumno.activo ?? false, // ✅ Evita el error de tipo
        },
      }));

      setAsistencias(formattedData);
    } catch (err) {
      console.error("Error al cargar asistencias:", err);
      setError("No se pudieron cargar las asistencias.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAsistencias();
  }, [fetchAsistencias]);

  const eliminarAsistencia = async (id: number) => {
    try {
      await asistenciasApi.eliminarAsistencia(id);
      setAsistencias((prev) =>
        prev.filter((asistencia) => asistencia.id !== id)
      );
    } catch (err) {
      console.error("Error al eliminar asistencia:", err);
      setError("Error al eliminar asistencia.");
    }
  };

  const pageCount = useMemo(
    () => Math.ceil(asistencias.length / itemsPerPage),
    [asistencias.length]
  );

  const currentItems = useMemo(
    () =>
      asistencias.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [asistencias, currentPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-red-500">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      <div className="flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/asistencias/formulario")}
          className="page-button"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nueva Asistencia
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={[
            "ID",
            "Alumno",
            "Disciplina",
            "Profesor",
            "Fecha",
            "Presente",
            "Activo", // ✅ Nueva columna para mostrar el estado activo del alumno
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/asistencias/formulario?id=${fila.id}`)
                }
                className="page-button-secondary"
                aria-label={`Editar asistencia de ${fila.alumno.nombre} en ${fila.disciplina.nombre}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                onClick={() => eliminarAsistencia(fila.id)}
                className="page-button-danger"
                aria-label={`Eliminar asistencia de ${fila.alumno.nombre}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            `${fila.alumno.nombre} ${fila.alumno.apellido}`,
            fila.disciplina.nombre,
            fila.profesor
              ? `${fila.profesor.nombre} ${fila.profesor.apellido}`
              : "N/A",
            fila.fecha,
            fila.presente ? "Sí" : "No",
            fila.alumno.activo ? "Activo" : "Inactivo", // ✅ Mostrar estado del alumno
          ]}
        />
      </div>
      {pageCount > 1 && (
        <ReactPaginate
          previousLabel={"← Anterior"}
          nextLabel={"Siguiente →"}
          breakLabel={"..."}
          pageCount={pageCount}
          onPageChange={handlePageClick}
          containerClassName={"pagination"}
          activeClassName={"active"}
          disabledClassName={"disabled"}
        />
      )}
    </div>
  );
};

export default Asistencias;
