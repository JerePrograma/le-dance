import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Asistencia {
  id: number;
  alumno: string;
  disciplina: string;
  fecha: string;
  presente: boolean;
  profesor?: string;
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
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Registro de Asistencias</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/asistencias/formulario")}
          className="page-button"
          aria-label="Registrar nueva asistencia"
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
                aria-label={`Editar asistencia de ${fila.alumno} en ${fila.disciplina}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar asistencia de ${fila.alumno}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.alumno,
            fila.disciplina,
            fila.profesor || "N/A",
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
