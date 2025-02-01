import { useEffect, useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../utilidades/alumnosApi";
import ReactPaginate from "react-paginate";
import Boton from "../../componentes/comunes/Boton";

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
  const itemsPerPage = 5; // Cantidad de items por página
  const navigate = useNavigate();

  // Fetch de datos optimizado con manejo de loading y error
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

  // Uso de useMemo para calcular el número total de páginas
  const pageCount = useMemo(
    () => Math.ceil(alumnos.length / itemsPerPage),
    [alumnos.length, itemsPerPage]
  );

  // Uso de useMemo para obtener los items correspondientes a la página actual
  const currentItems = useMemo(() => {
    return alumnos.slice(
      currentPage * itemsPerPage,
      (currentPage + 1) * itemsPerPage
    );
  }, [alumnos, currentPage, itemsPerPage]);

  // Función de paginación memorizada con useCallback
  const handlePageClick = useCallback(
    ({ selected }: { selected: number }) => {
      if (selected < pageCount) {
        setCurrentPage(selected);
      }
    },
    [pageCount]
  );

  // Si lo deseas, puedes mostrar un spinner o un mensaje de carga
  if (loading) return <div>Cargando...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div className="page-container">
      <h1 className="page-title">Alumnos</h1>

      <div className="flex justify-end mb-4">
        <Boton onClick={() => navigate("/alumnos/formulario")}>
          Ficha Aade Alumnos
        </Boton>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Apellido", "Acciones"]}
          datos={currentItems}
          // Puedes optimizar la función de renderizado de acciones usando useCallback en el componente Tabla o incluso envolver Tabla en React.memo
          acciones={(fila) => (
            <Boton
              onClick={() => navigate(`/alumnos/formulario?id=${fila.id}`)}
              secondary
              aria-label={`Editar alumno ${fila.nombre} ${fila.apellido}`}
            >
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
