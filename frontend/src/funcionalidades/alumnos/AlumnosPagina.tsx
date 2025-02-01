import { useEffect, useState, useCallback } from "react";
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
  const itemsPerPage = 5; // 🔄 Ya es una constante, no necesita useState
  const navigate = useNavigate();

  // 🔄 Fetch de datos optimizado
  const fetchAlumnos = useCallback(async () => {
    try {
      const response = await alumnosApi.listarAlumnos();
      setAlumnos(response);
    } catch (error) {
      console.error("Error al cargar alumnos:", error);
    }
  }, []);

  useEffect(() => {
    fetchAlumnos();
  }, [fetchAlumnos]);

  // 🔄 Cálculo seguro de paginación
  const pageCount = Math.ceil(alumnos.length / itemsPerPage);
  const currentItems = alumnos.slice(
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

      {/* 🔄 Paginación Mejorada */}
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
