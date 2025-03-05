import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import type { UsuarioResponse } from "../../types/types";
import usuariosApi from "../../api/usuariosApi";

const UsuariosPagina = () => {
  const [usuarios, setUsuarios] = useState<UsuarioResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchUsuarios = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const usuariosData = await usuariosApi.listarUsuarios();
      setUsuarios(usuariosData);
    } catch (err) {
      console.error("Error al cargar usuarios:", err);
      setError("Error al cargar usuarios.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsuarios();
  }, [fetchUsuarios]);

  const pageCount = useMemo(() => Math.ceil(usuarios.length / itemsPerPage), [usuarios.length]);
  const currentItems = useMemo(
    () => usuarios.slice(currentPage * itemsPerPage, (currentPage + 1) * itemsPerPage),
    [usuarios, currentPage]
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
        await usuariosApi.eliminarUsuario(id);
        setUsuarios((prev) => prev.filter((u) => u.id !== id));
      } catch (err) {
        console.error("Error al eliminar usuario:", err);
      }
    }
  }, []);

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error) return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Usuarios</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/usuarios/formulario")}
          className="page-button"
          aria-label="Registrar nuevo usuario"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Usuario
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={["ID", "Nombre", "Rol", "Activo", "Acciones"]}
          datos={currentItems}
          acciones={(fila: UsuarioResponse) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/usuarios/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar usuario ${fila.nombreUsuario}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                onClick={() => handleEliminarUsuario(fila.id)}
                className="page-button-danger"
                aria-label={`Eliminar usuario ${fila.nombreUsuario}`}
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila: UsuarioResponse) => [
            fila.id,
            fila.nombreUsuario,
            fila.rol,
            fila.activo ? "Activo" : "Inactivo",
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

export default UsuariosPagina;
