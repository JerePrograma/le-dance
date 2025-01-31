import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Tabla from "../../componentes/comunes/Tabla";
import alumnosApi from "../../utilidades/alumnosApi";
import ReactPaginate from "react-paginate";

interface AlumnoListado {
  id: number;
  nombre: string;
  apellido: string;
}

const Alumnos = () => {
  const [alumnos, setAlumnos] = useState<AlumnoListado[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [itemsPerPage] = useState(5); // Cantidad de alumnos por página

  const navigate = useNavigate();

  useEffect(() => {
    const fetchAlumnos = async () => {
      try {
        const response = await alumnosApi.listarAlumnos();
        setAlumnos(response);
      } catch (error) {
        console.error("Error al cargar alumnos:", error);
      }
    };
    fetchAlumnos();
  }, []);

  // Obtener los alumnos de la página actual
  const offset = currentPage * itemsPerPage;
  const currentItems = alumnos.slice(offset, offset + itemsPerPage);
  const pageCount = Math.ceil(alumnos.length / itemsPerPage);

  const handlePageClick = ({ selected }: { selected: number }) => {
    setCurrentPage(selected);
  };

  return (
    <div className="page-container">
      <h1 className="page-title">Alumnos</h1>

      <div className="flex justify-end mb-4">
        <button
          onClick={() => navigate("/alumnos/formulario")}
          className="page-button"
        >
          Ficha de Alumnos
        </button>
      </div>

      <div className="page-table-container">
        <Tabla
          encabezados={["ID", "Nombre", "Apellido", "Acciones"]}
          datos={currentItems}
          acciones={(fila) => (
            <button
              onClick={() => navigate(`/alumnos/formulario?id=${fila.id}`)}
              className="page-button bg-blue-500 hover:bg-blue-600"
            >
              Editar
            </button>
          )}
          extraRender={(fila) => [fila.id, fila.nombre, fila.apellido]}
        />
      </div>

      {/* Paginación */}
      <ReactPaginate
        previousLabel={"← Anterior"}
        nextLabel={"Siguiente →"}
        breakLabel={"..."}
        pageCount={pageCount}
        onPageChange={handlePageClick}
        containerClassName={"pagination"}
        activeClassName={"active"}
      />
    </div>
  );
};

export default Alumnos;
