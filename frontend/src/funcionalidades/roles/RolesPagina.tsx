import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../api/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";

interface Rol {
  id: number;
  descripcion: string;
}

const RolesPagina = () => {
  const [roles, setRoles] = useState<Rol[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchRoles = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get<Rol[]>("/api/roles");
      setRoles(response.data);
    } catch (error) {
      console.error("Error al cargar roles:", error);
      setError("Error al cargar roles.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

  const pageCount = useMemo(
    () => Math.ceil(roles.length / itemsPerPage),
    [roles.length]
  );
  const currentItems = useMemo(
    () =>
      roles.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [roles, currentPage]
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
      <h1 className="page-title">Roles</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/roles/formulario")}
          className="page-button"
          aria-label="Registrar Nuevo Rol"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Rol
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={["ID", "Descripción", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/roles/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar rol ${fila.descripcion}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar rol ${fila.descripcion}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [fila.id, fila.descripcion]}
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

export default RolesPagina;
