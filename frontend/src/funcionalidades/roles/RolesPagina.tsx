import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Rol {
  id: number;
  descripcion: string;
}

const RolesPagina = () => {
  const [roles, setRoles] = useState<Rol[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchRoles = useCallback(async () => {
    try {
      const response = await api.get<Rol[]>("/api/roles");
      setRoles(response.data);
    } catch (error) {
      console.error("Error al cargar roles:", error);
    }
  }, []);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

  const pageCount = Math.ceil(roles.length / itemsPerPage);
  const currentItems = roles.slice(
    currentPage * itemsPerPage,
    (currentPage + 1) * itemsPerPage
  );

  const handlePageClick = ({ selected }: { selected: number }) => {
    if (selected < pageCount) {
      setCurrentPage(selected);
    }
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Roles</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/roles/formulario")}>
          Registrar Nuevo Rol
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Descripción", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/roles/formulario?id=${fila.id}`)}
                secondary
                aria-label={`Editar rol ${fila.descripcion}`}
              >
                Editar
              </Boton>
              <Boton
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar rol ${fila.descripcion}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
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
