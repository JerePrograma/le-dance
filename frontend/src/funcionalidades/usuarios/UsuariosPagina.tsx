import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import api from "../../utilidades/axiosConfig";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: string;
}

const UsuariosPagina = () => {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchUsuarios = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.get<Usuario[]>("/api/usuarios");
      setUsuarios(response.data);
    } catch (error) {
      console.error("Error al cargar usuarios:", error);
      setError("Error al cargar usuarios.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsuarios();
  }, [fetchUsuarios]);

  const pageCount = useMemo(
    () => Math.ceil(usuarios.length / itemsPerPage),
    [usuarios.length, itemsPerPage]
  );
  const currentItems = useMemo(
    () =>
      usuarios.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [usuarios, currentPage, itemsPerPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  const handleEliminarUsuario = useCallback(async (id: number) => {
    if (window.confirm("¿Seguro que deseas eliminar este usuario?")) {
      try {
        await api.delete(`/api/usuarios/${id}`);
        setUsuarios((prev) => prev.filter((u) => u.id !== id));
      } catch (error) {
        console.error("Error al eliminar usuario:", error);
      }
    }
  }, []);

  if (loading) return <div>Cargando...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Usuarios</h1>
      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/usuarios/formulario")}>
          Registrar Nuevo Usuario
        </Boton>
      </div>
      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Correo", "Rol", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => handleEliminarUsuario(fila.id)}
                secondary
                className="bg-red-500 hover:bg-red-600"
                aria-label={`Eliminar usuario ${fila.nombre}`}
              >
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.correo, fila.rol]}
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

export default UsuariosPagina;
