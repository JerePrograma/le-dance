import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../utilidades/alumnosApi";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";
import { PlusCircle, Pencil } from "lucide-react";

interface AlumnoListado {
  id: number;
  nombre: string;
  apellido: string;
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 5;
  const navigate = useNavigate();

  const fetchAlumnos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await alumnosApi.listarAlumnos();
      setAlumnos(response);
    } catch (error) {
      console.error("Error al cargar alumnos:", error);
      setError("Error al cargar alumnos.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAlumnos();
  }, [fetchAlumnos]);

  const pageCount = useMemo(
    () => Math.ceil(alumnos.length / itemsPerPage),
    [alumnos.length]
  );

  const currentItems = useMemo(() => {
    return alumnos.slice(
      currentPage * itemsPerPage,
      (currentPage + 1) * itemsPerPage
    );
  }, [alumnos, currentPage]);

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
      <h1 className="page-title">Alumnos</h1>

      <div className="flex justify-end mb-4">
        <Boton
          onClick={() => navigate("/alumnos/formulario")}
          className="page-button"
        >
          <PlusCircle className="w-5 h-5 mr-2" />
          Ficha de Alumnos
        </Boton>
      </div>

      <div className="bg-card rounded-lg shadow-md overflow-hidden">
        <Tabla
          encabezados={["ID", "Nombre", "Apellido", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <Boton
              onClick={() => navigate(`/alumnos/formulario?id=${fila.id}`)}
              className="page-button-secondary"
              aria-label={`Editar alumno ${fila.nombre} ${fila.apellido}`}
            >
              <Pencil className="w-4 h-4 mr-2" />
              Editar
            </Boton>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.apellido]}
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

export default Alumnos;
