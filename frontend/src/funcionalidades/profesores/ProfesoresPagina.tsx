import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import profesoresApi from "../../api/profesoresApi"; // ✅ Usar la API correcta
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil, Trash2 } from "lucide-react";
import { ProfesorListadoResponse } from "../../types/types"; // ✅ Importar el tipo correcto
import { toast } from "react-toastify";

const itemsPerPage = 5;

const Profesores = () => {
  const [profesores, setProfesores] = useState<ProfesorListadoResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const fetchProfesores = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await profesoresApi.listarProfesores(); // ✅ Llamar la API correcta
      setProfesores(response);
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

  const pageCount = useMemo(
    () => Math.ceil(profesores.length / itemsPerPage),
    [profesores.length]
  );

  const currentItems = useMemo(
    () =>
      profesores.slice(
        currentPage * itemsPerPage,
        (currentPage + 1) * itemsPerPage
      ),
    [profesores, currentPage]
  );

  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  const handleEliminarProfesor = async (id: number) => {
    try {
      await profesoresApi.eliminarProfesor(id);
      toast.success("Profesor eliminado correctamente.");
      fetchProfesores(); // ✅ Refrescar la lista despues de eliminar
    } catch (error) {
      toast.error("Error al eliminar el profesor.");
    }
  };

  if (loading) return <div className="text-center py-4">Cargando...</div>;
  if (error)
    return <div className="text-center py-4 text-destructive">{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Profesores</h1>
      <div className="page-button-group flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/profesores/formulario")}
          className="page-button"
          aria-label="Registrar nuevo profesor"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Registrar Nuevo Profesor
        </Boton>
      </div>
      <div className="page-card">
        <Tabla
          encabezados={[
            "ID",
            "Nombre",
            "Apellido",
            "Acciones",
            "Activo",
          ]}
          datos={currentItems}
          acciones={(fila) => (
            <div className="flex gap-2">
              <Boton
                onClick={() => navigate(`/profesores/formulario?id=${fila.id}`)}
                className="page-button-secondary"
                aria-label={`Editar profesor ${fila.nombre} ${fila.apellido}`}
              >
                <Pencil className="w-4 h-4 mr-2" />
                Editar
              </Boton>
              <Boton
                className="page-button-danger"
                aria-label={`Eliminar profesor ${fila.nombre} ${fila.apellido}`}
                onClick={() => handleEliminarProfesor(fila.id)} // ✅ Llamar funcion para eliminar
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Eliminar
              </Boton>
            </div>
          )}
          extraRender={(fila) => [
            fila.id,
            fila.nombre,
            fila.apellido,
            fila.activo ? "Si" : "No", // ✅ Mostrar si esta activo o no
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

export default Profesores;
