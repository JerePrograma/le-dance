import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Profesor {
  id: number;
  nombre: string;
  apellido: string;
  especialidad: string;
  aniosExperiencia: number;
}

const Profesores = () => {
  const navigate = useNavigate();
  const [profesores, setProfesores] = useState<Profesor[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;

  const fetchProfesores = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get<Profesor[]>("/api/profesores");
      setProfesores(response.data);
    } catch (error) {
      console.error("Error al cargar profesores:", error);
      setError("Error al cargar profesores.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProfesores();
  }, [fetchProfesores]);

  const currentItems = profesores.slice(
    currentPage * itemsPerPage,
    (currentPage + 1) * itemsPerPage
  );

  const pageCount = Math.ceil(profesores.length / itemsPerPage);

  const handlePageClick = ({ selected }: { selected: number }) => {
    setCurrentPage(selected);
  };

  if (loading)
    return <div className="page-container text-center">Cargando...</div>;
  if (error)
    return <div className="page-container text-center text-error">{error}</div>;

  return (
    <div className="page-container">
      <div className="page-content">
        <h1 className="page-title">Profesores</h1>

        <div className="flex justify-end mb-6">
          <button
            onClick={() => navigate("/profesores/formulario")}
            className="button-primary"
          >
            <PlusCircle className="w-5 h-5" />
            <span>Registrar Nuevo Profesor</span>
          </button>
        </div>

        <div className="table-container">
          <table className="table">
            <thead className="table-header">
              <tr>
                <th className="table-header-cell">ID</th>
                <th className="table-header-cell">Nombre</th>
                <th className="table-header-cell">Apellido</th>
                <th className="table-header-cell">Especialidad</th>
                <th className="table-header-cell">Años de Experiencia</th>
                <th className="table-header-cell">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {currentItems.map((profesor) => (
                <tr key={profesor.id} className="table-row">
                  <td className="table-cell">{profesor.id}</td>
                  <td className="table-cell">{profesor.nombre}</td>
                  <td className="table-cell">{profesor.apellido}</td>
                  <td className="table-cell">{profesor.especialidad}</td>
                  <td className="table-cell">{profesor.aniosExperiencia}</td>
                  <td className="table-cell">
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() =>
                          navigate(`/profesores/formulario?id=${profesor.id}`)
                        }
                        className="button-secondary"
                      >
                        <Pencil className="w-4 h-4" />
                        <span>Editar</span>
                      </button>
                      <button
                        onClick={() => {
                          /* handle delete */
                        }}
                        className="button-danger"
                      >
                        <Trash2 className="w-4 h-4" />
                        <span>Eliminar</span>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {pageCount > 1 && (
          <ReactPaginate
            previousLabel={"← Anterior"}
            nextLabel={"Siguiente →"}
            breakLabel={"..."}
            pageCount={pageCount}
            onPageChange={handlePageClick}
            containerClassName="pagination"
            pageClassName="pagination-item"
            pageLinkClassName="pagination-link"
            activeClassName="pagination-active"
            disabledClassName="opacity-50 cursor-not-allowed"
            breakClassName="pagination-item"
            breakLinkClassName="pagination-link"
            previousClassName="pagination-item"
            previousLinkClassName="pagination-link"
            nextClassName="pagination-item"
            nextLinkClassName="pagination-link"
          />
        )}
      </div>
    </div>
  );
};

export default Profesores;
