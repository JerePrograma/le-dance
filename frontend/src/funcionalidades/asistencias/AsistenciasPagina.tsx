import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Asistencia {
  id: number;
  alumno: string;
  disciplina: string;
  fecha: string;
  presente: boolean;
  profesor?: string; // ✅ Se agrega el profesor
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
      setError(null);
      const response = await api.get<Asistencia[]>("/api/asistencias");
      setAsistencias(response.data);
    } catch (error) {
      console.error("Error al cargar asistencias:", error);
      setError("Error al cargar asistencias.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAsistencias();
  }, [fetchAsistencias]);

  const pageCount = useMemo(
    () => Math.ceil(asistencias.length / itemsPerPage),
    [asistencias.length, itemsPerPage]
  );
  const currentItems = useMemo(
    () =>
      asistencias.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [asistencias, currentPage, itemsPerPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  if (loading) return <div>Cargando...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/asistencias/formulario")}>
          Registrar Nueva Asistencia
        </Boton>
      </div>
      <div className="page-table-container">
        <Tabla
          encabezados={[
            "ID",
            "Alumno",
            "Disciplina",
            "Profesor", // ✅ Nuevo encabezado
            "Fecha",
            "Presente",
            "Acciones",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() =>
                  navigate(`/asistencias/formulario?id=${fila.id}`)
                }
                secondary
                aria-label={`Editar asistencia de ${fila.alumno} en ${fila.disciplina}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar asistencia de ${fila.alumno}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.alumno,
            fila.disciplina,
            fila.profesor || "N/A", // ✅ Mostramos el profesor o "N/A" si no hay
            fila.fecha,
            fila.presente ? "Sí" : "No",
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
